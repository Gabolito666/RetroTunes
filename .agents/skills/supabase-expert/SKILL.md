---
name: supabase-expert
description: Expert Supabase architecture and engineering. Specializes in Supabase client configuration, Row Level Security (RLS), access policies, database triggers, versioned migrations, Supabase Storage, and Edge Functions. Use when designing, securing, or integrating Supabase backends.
---

# Supabase Expert

## Overview

Comprehensive guide and operational runbook for architecting, securing, and integrating Supabase across modern web and backend stacks (Spring Boot, Node.js, Next.js, and client-side applications).

---

## 1. Client Configuration & Security Boundaries

### Key Management & Roles
- **anon key (Public)**: Safe for client-side applications (browser, mobile). Governed strictly by Postgres Row Level Security (RLS). Never expose `service_role` on the client.
- **service_role key (Secret)**: Bypasses ALL Row Level Security policies. Must ONLY be used in secure server environments (e.g., Spring Boot backend, confidential cron jobs, admin workers). Never check into version control; load via environment variables (`SUPABASE_SERVICE_ROLE_KEY`).
- **JWT Secret**: Used for verifying authentication tokens issued by Supabase Auth (GoTrue).

### Environment Variable Standards
```env
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOi...
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOi...
SUPABASE_DB_URL=postgresql://postgres:[PASSWORD]@db.<project-ref>.supabase.co:5432/postgres
```

### Backend Integration (Java / Spring Boot)
When interacting with Supabase from Spring Boot:
- Use PostgreSQL JDBC with HikariCP connected to Supabase Database (or Supabase Connection Pooler / PgBouncer port 6543 for transaction pooling).
- Validate Supabase JWTs in Spring Security using `oauth2ResourceServer(jwt -> jwt.jwkSetUri(...))` or signing key verification.
- Propagate user identity or use RLS-aware connections via `SET LOCAL "request.jwt.claim.sub" = :userId` when using direct SQL connections under client context.

---

## 2. Row Level Security (RLS) & Access Policies

### Golden Rules
1. **Always enable RLS** on every public table:
   ```sql
   ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
   ```
2. By default, enabling RLS denies all access unless explicit policies are granted.
3. Optimize policy evaluations: avoid subqueries inside RLS where possible; ensure columns checked in policies (such as `user_id` or `tenant_id`) are indexed with B-Tree indexes.

### Policy Patterns

#### Authenticated User Ownership
```sql
-- Read own profile
CREATE POLICY "Users can read own profile"
ON public.profiles
FOR SELECT
TO authenticated
USING (auth.uid() = id);

-- Update own profile
CREATE POLICY "Users can update own profile"
ON public.profiles
FOR UPDATE
TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);
```

#### Multi-Tenant / Team Isolation
```sql
CREATE POLICY "Team members can view team data"
ON public.team_documents
FOR SELECT
TO authenticated
USING (
  team_id IN (
    SELECT tm.team_id 
    FROM public.team_members tm 
    WHERE tm.user_id = auth.uid()
  )
);
```
> **Performance Optimization**: For high-throughput tables, cache team memberships in JWT claims or use a `security definer` function to prevent recursive scans.

#### Public Read / Authenticated Write
```sql
CREATE POLICY "Public items can be viewed by anyone"
ON public.catalog_items
FOR SELECT
TO anon, authenticated
USING (is_published = true);

CREATE POLICY "Authenticated users can create items"
ON public.catalog_items
FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = created_by);
```

---

## 3. Database Triggers & Functions

### Sync Auth Users to Public Profiles
Automatically populate `public.profiles` upon user signup in Supabase Auth:

```sql
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER 
SECURITY DEFINER 
SET search_path = public
LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO public.profiles (id, email, full_name, avatar_url, created_at, updated_at)
  VALUES (
    NEW.id,
    NEW.email,
    NEW.raw_user_meta_data->>'full_name',
    NEW.raw_user_meta_data->>'avatar_url',
    NOW(),
    NOW()
  );
  RETURN NEW;
END;
$$;

-- Trigger fires after user creation in auth schema
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
```

### Auto-updating `updated_at` Timestamp
```sql
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER 
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

CREATE TRIGGER set_profiles_updated_at
  BEFORE UPDATE ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
```

---

## 4. Database Migrations (Supabase CLI & Flyway)

### Supabase CLI Workflow
1. **Initialize Supabase**: `supabase init`
2. **Start Local Stack**: `supabase start` (runs local Postgres, GoTrue, PostgREST, Inbucket, Storage)
3. **Generate Migration**:
   ```bash
   supabase migration new create_profiles_table
   ```
4. **Diff Migration against Local DB**:
   ```bash
   supabase db diff -f add_new_feature
   ```
5. **Apply Migrations**:
   ```bash
   supabase db reset      # local reset
   supabase db push       # push to remote staging/production
   ```

### Flyway Integration (Spring Boot / Maven)
If managing schema versions via Maven / Spring Boot Flyway:
- Place migrations in `src/main/resources/db/migration/` (`V1__initial_schema.sql`, `V2__add_rls_policies.sql`).
- Maintain idempotent DDL scripts.
- Ensure Supabase Postgres roles (`postgres`, `service_role`, `authenticated`, `anon`) are properly handled in SQL scripts.

---

## 5. Supabase Storage Policies & Buckets

- Create private buckets for sensitive user uploads and public buckets for public media:
```sql
-- Insert bucket definition
INSERT INTO storage.buckets (id, name, public) 
VALUES ('avatars', 'avatars', false);

-- Storage RLS: Allow authenticated users to upload to their own folder
CREATE POLICY "Users can upload their own avatars"
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'avatars' AND 
  (storage.foldername(name))[1] = auth.uid()::text
);

-- Storage RLS: Allow users to view their own avatars
CREATE POLICY "Users can read their own avatars"
ON storage.objects
FOR SELECT
TO authenticated
USING (
  bucket_id = 'avatars' AND 
  (storage.foldername(name))[1] = auth.uid()::text
);
```

---

## 6. Edge Functions

- Developed using TypeScript on the Deno runtime.
- Use for lightweight webhook endpoints, third-party payment callbacks (Stripe), and background compute.
- Always validate authorization headers and handle CORS headers:
```typescript
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }
  
  const supabaseClient = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_ANON_KEY') ?? '',
    { global: { headers: { Authorization: req.headers.get('Authorization')! } } }
  )

  const { data: { user }, error } = await supabaseClient.auth.getUser()
  if (error || !user) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), {
      status: 401,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }

  return new Response(JSON.stringify({ message: `Hello ${user.email}` }), {
    headers: { ...corsHeaders, 'Content-Type': 'application/json' }
  })
})
```

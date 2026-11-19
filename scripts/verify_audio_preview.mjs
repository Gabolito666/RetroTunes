import { execSync } from "node:child_process";
import { existsSync, mkdirSync } from "node:fs";
import { resolve, dirname } from "node:path";

export function inspectAudio(filePath) {
    const absPath = resolve(filePath);
    if (!existsSync(absPath)) {
        throw new Error(`Audio file not found: ${absPath}`);
    }

    const cmd = `ffprobe -v error -show_entries format=duration,bit_rate,format_name:stream=codec_name,sample_rate,channels -of json "${absPath}"`;
    const raw = execSync(cmd, { encoding: "utf8" });
    const probe = JSON.parse(raw);

    const stream = probe.streams?.[0] || {};
    const format = probe.format || {};

    const durationSec = parseFloat(format.duration || "0");
    const bitRateKbps = Math.round(parseInt(format.bit_rate || "0", 10) / 1000);

    return {
        file: absPath,
        codec: stream.codec_name || "unknown",
        sampleRateHz: stream.sample_rate || "unknown",
        channels: stream.channels || 0,
        durationSeconds: durationSec,
        bitRateKbps: bitRateKbps,
        isValidPreview: durationSec >= 28.0 && durationSec <= 32.0,
        summary: `${stream.codec_name?.toUpperCase()} @ ${bitRateKbps} kbps, ${durationSec.toFixed(1)}s, ${stream.sample_rate}Hz`
    };
}

export function generateSyntheticPreview(outputPath, duration = 30) {
    const absOut = resolve(outputPath);
    mkdirSync(dirname(absOut), { recursive: true });
    // FFmpeg recipe: generate 30s 440Hz warm vinyl-style tone with sine filter
    const cmd = `ffmpeg -y -f lavfi -i "sine=frequency=440:duration=${duration}" -c:a libmp3lame -b:a 128k -ar 44100 "${absOut}"`;
    execSync(cmd, { stdio: "ignore" });
    return inspectAudio(absOut);
}

// CLI usage
if (process.argv[1]?.endsWith("verify_audio_preview.mjs")) {
    const target = process.argv[2];
    if (!target) {
        console.log("Usage: node scripts/verify_audio_preview.mjs <audio_file.mp3> [--generate]");
        console.log("Generating sample preview in src/main/resources/static/audio/sample-preview.mp3...");
        const result = generateSyntheticPreview("src/main/resources/static/audio/sample-preview.mp3", 30);
        console.log(JSON.stringify(result, null, 2));
    } else {
        const result = inspectAudio(target);
        console.log(JSON.stringify(result, null, 2));
    }
}

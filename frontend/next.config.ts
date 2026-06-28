import type { NextConfig } from "next";
import fs from "fs";
import path from "path";

// Load environment variables from the repository root .env file if it exists
const envPath = path.resolve(process.cwd(), "../.env");
if (fs.existsSync(envPath)) {
  const envContent = fs.readFileSync(envPath, "utf-8");
  envContent.split(/\r?\n/).forEach((line) => {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      return;
    }
    const eqIdx = trimmed.indexOf("=");
    if (eqIdx > 0) {
      const key = trimmed.slice(0, eqIdx).trim();
      let value = trimmed.slice(eqIdx + 1).trim();
      if (
        (value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))
      ) {
        value = value.slice(1, -1);
      }
      if (process.env[key] === undefined) {
        process.env[key] = value;
      }
    }
  });
}

const nextConfig: NextConfig = {
  // Enables standalone output: bundles only the minimal server and its
  // dependencies into .next/standalone/. The Docker runtime stage copies
  // only that directory, keeping the image small and free of dev deps.
  output: "standalone",
};

export default nextConfig;

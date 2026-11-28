import { defineConfig } from "vite";
import wasm from "vite-plugin-wasm";

export default defineConfig({
  base: "./",
  plugins: [wasm()],
  optimizeDeps: {
    exclude: ["@midnight-ntwrk/compact-runtime", "@midnightntwrk/onchain-runtime-v4"],
    include: ["@midnight-ntwrk/compact-runtime > object-inspect"],
  },
});

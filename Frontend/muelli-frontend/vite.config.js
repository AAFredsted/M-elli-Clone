import { defineConfig } from "vite";
import { viteStaticCopy } from "vite-plugin-static-copy";
import path from "path";

export default defineConfig({
	resolve: {
		alias: {
			"@": path.resolve(__dirname, "./src"),
		},
	},
	plugins: [
		viteStaticCopy({
			targets: [
				{
					src: path.resolve(__dirname, "src/data"),
					dest: "",
				},
				{
					src: path.resolve(__dirname, "src/icons"),
					dest: "",
				},
			],
		}),
	],
	build: {
		sourcemap: true,
	},
});

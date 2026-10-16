"""Build an unsigned LSPosed APK using JDK 17, aapt2 and D8."""
from pathlib import Path
import argparse
import os
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", default=os.environ.get("ALIPAY_FIX_JDK") or os.environ.get("JAVA_HOME"))
    parser.add_argument("--aapt2", default=os.environ.get("AAPT2") or shutil.which("aapt2"))
    parser.add_argument("--android-jar", type=Path, default=ROOT / "deps/android.jar")
    parser.add_argument("--xposed-api", type=Path, default=ROOT / "deps/xposed-api.jar")
    parser.add_argument("--r8", type=Path, default=ROOT / "deps/r8.jar")
    args = parser.parse_args()
    suffix = ".exe" if os.name == "nt" else ""
    tools = {}
    for name in ["java", "javac"]:
        tool = str(Path(args.jdk) / "bin" / (name + suffix)) if args.jdk else shutil.which(name)
        if not tool or not Path(tool).is_file():
            parser.error("JDK 17 required: set JAVA_HOME or provide --jdk.")
        tools[name] = tool
    if not args.aapt2 or not Path(args.aapt2).is_file():
        parser.error("Provide --aapt2 PATH or put Android SDK aapt2 on PATH.")
    for dependency in [args.android_jar, args.xposed_api, args.r8]:
        if not dependency.is_file():
            parser.error("Missing dependency: " + str(dependency))
    build = ROOT / "build"
    for folder in [build / "classes", build / "dex"]:
        folder.mkdir(parents=True, exist_ok=True)
        for generated in folder.rglob("*"):
            if generated.is_file() and generated.suffix in {".class", ".dex"}:
                generated.unlink()

    def run(*command):
        subprocess.run([str(value) for value in command], cwd=ROOT, check=True)

    run(tools["javac"], "-encoding", "UTF-8", "--release", "8", "-classpath",
        str(args.android_jar.resolve()) + os.pathsep + str(args.xposed_api.resolve()),
        "-d", build / "classes", *sorted((ROOT / "src").rglob("*.java")))
    classes_jar = build / "module.jar"
    with zipfile.ZipFile(classes_jar, "w") as archive:
        for source in sorted((build / "classes").rglob("*.class")):
            archive.write(source, source.relative_to(build / "classes").as_posix())
    run(tools["java"], "-cp", args.r8.resolve(), "com.android.tools.r8.D8", "--min-api", "33",
        "--lib", args.android_jar.resolve(), "--classpath", args.xposed_api.resolve(),
        "--output", build / "dex", classes_jar)
    run(args.aapt2, "compile", "--dir", ROOT / "res", "-o", build / "resources.zip")
    unsigned = build / "alipay-resource-fix-unsigned.apk"
    run(args.aapt2, "link", "-I", args.android_jar.resolve(), "--manifest", ROOT / "AndroidManifest.xml",
        "-o", unsigned, build / "resources.zip")
    with zipfile.ZipFile(unsigned, "a", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(build / "dex/classes.dex", "classes.dex")
        archive.write(ROOT / "assets/xposed_init", "assets/xposed_init")
    print("Unsigned APK:", unsigned)
    print("Align and sign with Android SDK zipalign/apksigner before installing.")

if __name__ == "__main__":
    main()

import os
import stat
import subprocess
import sys

def ensure_executable(path: str):
    if os.path.exists(path):
        current_stat = os.stat(path)
        os.chmod(path, current_stat.st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)

def run_command(command, cwd=None):
    if isinstance(command, list) and command and command[0] == "./gradlew":
        ensure_executable(os.path.join(cwd or ".", "gradlew"))
    elif isinstance(command, str) and command.startswith("./gradlew"):
        ensure_executable(os.path.join(cwd or ".", "gradlew"))

    try:
        result = subprocess.run(
            command,
            cwd=cwd,
            shell=isinstance(command, str),
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        return result.returncode, result.stdout, result.stderr
    except PermissionError:
        # Fallback z wymuszeniem wywołania przez sh w przypadku restrykcji uprawnień
        if isinstance(command, list) and command and command[0] == "./gradlew":
            fallback_cmd = ["sh", "gradlew"] + command[1:]
        elif isinstance(command, str) and command.startswith("./gradlew"):
            fallback_cmd = "sh " + command
        else:
            raise

        result = subprocess.run(
            fallback_cmd,
            cwd=cwd,
            shell=isinstance(fallback_cmd, str),
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        return result.returncode, result.stdout, result.stderr

if __name__ == "__main__":
    ensure_executable("gradlew")
    cmd = ["sh", "./gradlew", "test"] if len(sys.argv) <= 1 else sys.argv[1:]
    code, out, err = run_command(cmd)
    if out:
        print(out)
    if err:
        print(err, file=sys.stderr)
    sys.exit(code)
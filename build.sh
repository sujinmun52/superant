#!/usr/bin/env bash
# 슈퍼개미 빌드 스크립트: 테스트 -> 실행 jar -> Javadoc
# 필요: JDK 17 이상 (javac, jar, javadoc)
set -euo pipefail
cd "$(dirname "$0")"

ID=ssd_ip_cp4_2026f_244489
# JAVA_HOME 의 bin 을 우선 사용 (jar, javadoc 이 PATH 에 없을 수 있음)
if [ -n "${JAVA_HOME:-}" ]; then PATH="$JAVA_HOME/bin:$PATH"; fi
if ! command -v jar >/dev/null 2>&1; then
  for d in "/c/Program Files/Java/latest/bin" /c/Program\ Files/Java/jdk-*/bin; do
    [ -x "$d/jar.exe" ] && PATH="$d:$PATH" && break
  done
fi
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
rm -rf build/classes build/test-classes build/api dist
mkdir -p build/classes build/test-classes dist

echo "[1/4] compile"
javac -encoding UTF-8 --release 17 -d build/classes $(find src -name '*.java')

echo "[2/4] test"
javac -encoding UTF-8 --release 17 -cp build/classes -d build/test-classes test/superant/RuleTest.java
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "build/classes${SEP}build/test-classes" superant.RuleTest

echo "[3/4] jar"
printf 'Main-Class: superant.view.StockGameApp\n' > build/manifest.txt
jar cfm "dist/${ID}_app.jar" build/manifest.txt -C build/classes .

echo "[4/4] javadoc"
javadoc -quiet -encoding UTF-8 -charset UTF-8 -docencoding UTF-8 -Xdoclint:all,-missing -author -version \
  -windowtitle "슈퍼개미 API" -doctitle "슈퍼개미 (가상 주식 투자 시뮬레이션 게임) API" \
  -d build/api -sourcepath src -subpackages superant
echo "done: dist/${ID}_app.jar, build/api/index.html"

#!/bin/bash
# Crea el instalador descargable de TaskFlow (dist/TaskFlow.dmg).
#
# Compila en Release, optimizado en tamaño, para Apple Silicon e Intel, firma con firma "ad hoc"
# (no requiere cuenta de desarrollador de pago) y empaqueta un .dmg con la app y un acceso a Aplicaciones.
#
# Uso:  Tools/crear-instalador.sh
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ"
SALIDA="$RAIZ/dist"
TRABAJO="$(mktemp -d)"
trap 'rm -rf "$TRABAJO"' EXIT

echo "▸ Compilando (Release, universal, optimizado)…"
xcodebuild \
  -project GestorUniversitario.xcodeproj -scheme TaskFlow -configuration Release \
  -derivedDataPath "$TRABAJO/build" \
  ARCHS="arm64 x86_64" ONLY_ACTIVE_ARCH=NO \
  CODE_SIGNING_ALLOWED=NO \
  SWIFT_ACTIVE_COMPILATION_CONDITIONS=DIRECT_DISTRIBUTION \
  SWIFT_OPTIMIZATION_LEVEL=-O GCC_OPTIMIZATION_LEVEL=s \
  DEBUG_INFORMATION_FORMAT=dwarf COPY_PHASE_STRIP=YES STRIP_INSTALLED_PRODUCT=YES STRIP_STYLE=all \
  ENABLE_TESTABILITY=NO \
  build > "$TRABAJO/compilacion.log" 2>&1 || { tail -30 "$TRABAJO/compilacion.log"; echo "✗ Falló la compilación"; exit 1; }

APP="$TRABAJO/build/Build/Products/Release/TaskFlow.app"
WIDGETS="$APP/Contents/PlugIns/TaskFlowWidgets.appex"
VERSION="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$APP/Contents/Info.plist")"

echo "▸ Firmando (ad hoc, de dentro hacia fuera)…"
xattr -cr "$APP"
codesign --force --sign - --entitlements Config/Direct-Widgets.entitlements "$WIDGETS"
codesign --force --sign - --entitlements Config/Direct-App.entitlements "$APP"
codesign --verify --strict --deep "$APP"

echo "▸ Creando el .dmg…"
mkdir -p "$SALIDA"
PREPARACION="$TRABAJO/dmg"
mkdir -p "$PREPARACION"
cp -R "$APP" "$PREPARACION/"
ln -s /Applications "$PREPARACION/Aplicaciones"
rm -f "$SALIDA/TaskFlow.dmg" "$SALIDA/TaskFlow.dmg.sha256"
hdiutil create -volname "TaskFlow" -srcfolder "$PREPARACION" -fs HFS+ -format ULMO -ov "$SALIDA/TaskFlow.dmg" > /dev/null
( cd "$SALIDA" && shasum -a 256 TaskFlow.dmg > TaskFlow.dmg.sha256 )

echo
echo "✓ TaskFlow $VERSION"
echo "  $(du -h "$SALIDA/TaskFlow.dmg" | cut -f1)  $SALIDA/TaskFlow.dmg"
echo "  Arquitecturas: $(lipo -archs "$APP/Contents/MacOS/TaskFlow")"

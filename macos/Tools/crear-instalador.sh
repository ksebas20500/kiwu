#!/bin/bash
# Crea el instalador descargable de Kiwu (dist/Kiwu.dmg).
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
  -project Kiwu.xcodeproj -scheme Kiwu -configuration Release \
  -derivedDataPath "$TRABAJO/build" \
  ARCHS="arm64 x86_64" ONLY_ACTIVE_ARCH=NO \
  CODE_SIGNING_ALLOWED=NO \
  SWIFT_ACTIVE_COMPILATION_CONDITIONS=DIRECT_DISTRIBUTION \
  SWIFT_OPTIMIZATION_LEVEL=-O GCC_OPTIMIZATION_LEVEL=s \
  DEBUG_INFORMATION_FORMAT=dwarf COPY_PHASE_STRIP=YES STRIP_INSTALLED_PRODUCT=YES STRIP_STYLE=all \
  ENABLE_TESTABILITY=NO \
  build > "$TRABAJO/compilacion.log" 2>&1 || { tail -30 "$TRABAJO/compilacion.log"; echo "✗ Falló la compilación"; exit 1; }

APP="$TRABAJO/build/Build/Products/Release/Kiwu.app"
WIDGETS="$APP/Contents/PlugIns/KiwuWidgets.appex"
VERSION="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$APP/Contents/Info.plist")"

echo "▸ Firmando (ad hoc, de dentro hacia fuera)…"
xattr -cr "$APP"
codesign --force --sign - --entitlements Config/Direct-Widgets.entitlements "$WIDGETS"
codesign --force --sign - --entitlements Config/Direct-App.entitlements "$APP"
codesign --verify --strict --deep "$APP"

echo "▸ Creando el .dmg (con fondo y diseño de ventana)…"
mkdir -p "$SALIDA"
rm -f "$SALIDA/Kiwu.dmg" "$SALIDA/Kiwu.dmg.sha256"
VOLUMEN="Kiwu"
RW="$TRABAJO/rw.dmg"
PUNTO_DMG="/Volumes/$VOLUMEN"
hdiutil detach "$PUNTO_DMG" -quiet 2>/dev/null || true   # por si ya hay un instalador abierto
hdiutil create -size 40m -fs HFS+ -volname "$VOLUMEN" -ov "$RW" -quiet
hdiutil attach "$RW" -mountpoint "$PUNTO_DMG" -nobrowse -noverify -quiet
cp -R "$APP" "$PUNTO_DMG/"
ln -s /Applications "$PUNTO_DMG/Aplicaciones"

# Fondo de la ventana (si algo falla, el instalador sale igualmente, solo que más sencillo).
DISENO=1
swiftc Tools/fondo-dmg.swift -o "$TRABAJO/fondo-dmg" 2>/dev/null && mkdir -p "$PUNTO_DMG/.background" \
  && "$TRABAJO/fondo-dmg" "$PUNTO_DMG/.background/fondo.png" "$VERSION" || DISENO=0
if [ "$DISENO" = 1 ]; then
  osascript <<APPLESCRIPT >/dev/null 2>&1 || echo "  (aviso: no se pudo dar formato a la ventana; el .dmg funciona igual)"
tell application "Finder"
  tell disk "$VOLUMEN"
    open
    set current view of container window to icon view
    set toolbar visible of container window to false
    set statusbar visible of container window to false
    set the bounds of container window to {200, 120, 860, 548}
    set opciones to the icon view options of container window
    set arrangement of opciones to not arranged
    set icon size of opciones to 112
    set text size of opciones to 13
    set background picture of opciones to file ".background:fondo.png"
    set position of item "Kiwu.app" of container window to {180, 190}
    set position of item "Aplicaciones" of container window to {480, 190}
    close
    open
    update without registering applications
    delay 2
    close
  end tell
end tell
APPLESCRIPT
fi

sync
chmod -Rf go-w "$PUNTO_DMG" 2>/dev/null || true
hdiutil detach "$PUNTO_DMG" -quiet || { sleep 2; hdiutil detach "$PUNTO_DMG" -force -quiet; }
hdiutil convert "$RW" -format ULMO -o "$SALIDA/Kiwu.dmg" -ov -quiet
( cd "$SALIDA" && shasum -a 256 Kiwu.dmg > Kiwu.dmg.sha256 )

echo
echo "✓ Kiwu $VERSION"
echo "  $(du -h "$SALIDA/Kiwu.dmg" | cut -f1)  $SALIDA/Kiwu.dmg"
echo "  Arquitecturas: $(lipo -archs "$APP/Contents/MacOS/Kiwu")"

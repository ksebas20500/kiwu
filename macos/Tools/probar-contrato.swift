// Prueba de contrato: lee las fixtures de docs/contrato/notas con el modelo real de la app (NoteDocument)
// y comprueba que dan el mismo resultado que en Linux. Se ejecuta con Tools/probar-contrato.sh.
import Foundation

struct FalloContrato: Error { let mensaje: String }

@main
struct ProbarContrato {
    static func main() {
        let ruta = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "docs/contrato/notas"
        let carpeta = URL(fileURLWithPath: ruta)
        let archivos = ((try? FileManager.default.contentsOfDirectory(atPath: carpeta.path)) ?? [])
            .filter { $0.hasSuffix(".json") && !$0.hasSuffix(".esperado.json") }
            .sorted()
        guard !archivos.isEmpty else {
            print("✗ No hay fixtures en \(carpeta.path)")
            exit(1)
        }

        var fallos = 0
        for nombre in archivos {
            do {
                try comprobar(nombre, en: carpeta)
                print("✓ \(nombre)")
            } catch let error as FalloContrato {
                fallos += 1
                print("✗ \(nombre): \(error.mensaje)")
            } catch {
                fallos += 1
                print("✗ \(nombre): \(error)")
            }
        }
        print(fallos == 0 ? "Contrato cumplido (\(archivos.count) fixtures)" : "\(fallos) fixture(s) incumplen el contrato")
        exit(fallos == 0 ? 0 : 1)
    }

    static func resumen(_ doc: NoteDocument) -> (texto: String, tipos: [String], sangrias: [Int]) {
        (doc.textoPlano, doc.blocks.map { $0.kind.rawValue }, doc.blocks.map { $0.indent })
    }

    static func comprobar(_ nombre: String, en carpeta: URL) throws {
        let base = String(nombre.dropLast(".json".count))
        let datos = try Data(contentsOf: carpeta.appendingPathComponent(nombre))
        let doc = try JSONDecoder().decode(NoteDocument.self, from: datos)

        let datosEsperados = try Data(contentsOf: carpeta.appendingPathComponent(base + ".esperado.json"))
        guard let esperado = try JSONSerialization.jsonObject(with: datosEsperados) as? [String: Any],
              let texto = esperado["texto_plano"] as? String,
              let tipos = esperado["tipos"] as? [String],
              let sangrias = esperado["sangrias"] as? [Int] else {
            throw FalloContrato(mensaje: "el archivo .esperado.json no tiene el formato correcto")
        }

        let leido = resumen(doc)
        guard leido.texto == texto else { throw FalloContrato(mensaje: "texto_plano distinto: «\(leido.texto)»") }
        guard leido.tipos == tipos else { throw FalloContrato(mensaje: "tipos distintos: \(leido.tipos)") }
        guard leido.sangrias == sangrias else { throw FalloContrato(mensaje: "sangrías distintas: \(leido.sangrias)") }

        // Escribir y volver a leer no debe cambiar nada.
        let vuelta = try JSONDecoder().decode(NoteDocument.self, from: doc.data)
        let releido = resumen(vuelta)
        guard releido.texto == leido.texto, releido.tipos == leido.tipos, releido.sangrias == leido.sangrias else {
            throw FalloContrato(mensaje: "al reescribir y releer la nota cambia su contenido")
        }
    }
}

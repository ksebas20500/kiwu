import CoreData

/// Error legible al exportar o importar una copia de seguridad.
struct ErrorCopia: LocalizedError {
    let mensaje: String
    var errorDescription: String? { mensaje }
}

/// Lo que se añadió realmente al importar (las filas ya existentes o descartadas no cuentan).
struct ResumenImportacion: Equatable {
    var listas = 0, tareas = 0, cuadernos = 0, notas = 0, horarios = 0, clases = 0

    var total: Int { listas + tareas + cuadernos + notas + horarios + clases }

    var texto: String {
        "Se añadieron \(listas) listas, \(tareas) tareas, \(cuadernos) cuadernos, \(notas) notas, \(horarios) horarios y \(clases) clases."
    }
}

/// Copia de seguridad `.kiwu.json` (formato 1): el mismo archivo que exportan e importan Kiwu para Linux y Android,
/// así que sirve para mover los datos entre dispositivos. Al importar no se pisa lo existente ni se resucita lo eliminado.
/// Las carpetas vinculadas no se exportan: solo valen en el equipo donde se eligieron.
enum CopiaService {
    static let formato = 1

    // MARK: Fechas (ISO-8601 UTC sin fracciones: 2026-10-13T15:04:05Z)

    private static func formateador(_ opciones: ISO8601DateFormatter.Options) -> ISO8601DateFormatter {
        let f = ISO8601DateFormatter()
        f.formatOptions = opciones
        f.timeZone = TimeZone(identifier: "UTC")
        return f
    }

    static func iso(_ fecha: Date?) -> Any {
        guard let fecha else { return NSNull() }
        return formateador([.withInternetDateTime]).string(from: Date(timeIntervalSince1970: fecha.timeIntervalSince1970.rounded(.down)))
    }

    static func desdeIso(_ valor: Any?) -> Date? {
        guard let texto = valor as? String, !texto.isEmpty else { return nil }
        return formateador([.withInternetDateTime]).date(from: texto)
            ?? formateador([.withInternetDateTime, .withFractionalSeconds]).date(from: texto)
    }

    // MARK: Notas (objeto JSON o texto con JSON → formato canónico)

    private static func documento(_ valor: Any?) -> NoteDocument {
        var datos: Data?
        if let texto = valor as? String { datos = texto.data(using: .utf8) }
        else if let objeto = valor as? [String: Any], JSONSerialization.isValidJSONObject(objeto) { datos = try? JSONSerialization.data(withJSONObject: objeto) }
        guard let datos else { return .empty }
        return NotaService.leer(datos)
    }

    private static func comoJSON(_ datos: Data) -> Any {
        let doc = NotaService.leer(datos)
        return (try? JSONSerialization.jsonObject(with: doc.data)) ?? [:]
    }

    // MARK: Exportar

    /// Genera el contenido del archivo `.kiwu.json` con todo lo que sigue vivo.
    static func exportar(en contexto: NSManagedObjectContext, ahora: Date = .now) throws -> Data {
        var resultado: Data?
        var fallo: Error?
        contexto.performAndWait {
            do { resultado = try exportarDatos(contexto, ahora) } catch { fallo = error }
        }
        if let fallo { throw fallo }
        return resultado ?? Data()
    }

    private static func exportarDatos(_ contexto: NSManagedObjectContext, _ ahora: Date) throws -> Data {
        let materias = try contexto.fetch(ordenada(Materia.fetchRequest()))
        let deberes = try contexto.fetch(Deber.fetchRequest())
        let cuadernos = try contexto.fetch(ordenada(Cuaderno.fetchRequest()))
        let paginas = try contexto.fetch(Pagina.fetchRequest())
        let horarios = try contexto.fetch(ordenada(Horario.fetchRequest()))
        let clases = try contexto.fetch(Clase.fetchRequest())

        let raiz: [String: Any] = [
            "formato": formato,
            "app": "kiwu",
            "exportado": iso(ahora),
            "materias": materias.map { m -> [String: Any] in
                ["id": m.id.uuidString, "nombre": m.nombre, "orden": Int(m.orden), "carpeta": NSNull(),
                 "creada": iso(m.fechaCreacion), "actualizada": iso(m.fechaActualizacion)]
            },
            "deberes": deberes.map { d -> [String: Any] in
                ["id": d.id.uuidString, "materia": d.materia.id.uuidString, "titulo": d.titulo,
                 "completado": d.completado, "estado": Int(d.estado), "fechaEntrega": iso(d.fechaEntrega),
                 "recordatorioHoras": Int(d.recordatorioAntesEnHoras), "fechaCompletado": iso(d.fechaCompletado),
                 "nota": comoJSON(d.contenidoNota), "creada": iso(d.fechaCreacion), "actualizada": iso(d.fechaActualizacion)]
            },
            "cuadernos": cuadernos.map { c -> [String: Any] in
                ["id": c.id.uuidString, "nombre": c.nombre, "icono": c.icono, "orden": Int(c.orden),
                 "creada": iso(c.fechaCreacion), "actualizada": iso(c.fechaActualizacion)]
            },
            "paginas": paginas.map { p -> [String: Any] in
                ["id": p.id.uuidString, "cuaderno": p.cuaderno.id.uuidString, "titulo": p.titulo, "icono": p.icono,
                 "favorita": p.favorita, "contenido": comoJSON(p.contenido),
                 "creada": iso(p.fechaCreacion), "actualizada": iso(p.fechaActualizacion)]
            },
            "horarios": horarios.map { h -> [String: Any] in
                ["id": h.id.uuidString, "nombre": h.nombre, "icono": h.icono, "orden": Int(h.orden),
                 "creada": iso(h.fechaCreacion), "actualizada": iso(h.fechaActualizacion)]
            },
            "clases": clases.map { c -> [String: Any] in
                ["id": c.id.uuidString, "horario": c.horario.id.uuidString,
                 "materia": c.materiaId?.uuidString ?? NSNull(), "grupo": c.grupo.uuidString,
                 "titulo": c.titulo, "diaSemana": Int(c.diaSemana), "inicioMin": Int(c.inicioMin), "finMin": Int(c.finMin),
                 "aula": c.aula, "notas": c.notas, "avisoMin": c.aviso ?? NSNull(),
                 "creada": iso(c.fechaCreacion), "actualizada": iso(c.fechaActualizacion)]
            }
        ]
        return try JSONSerialization.data(withJSONObject: raiz, options: [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes])
    }

    private static func ordenada<T: NSManagedObject>(_ peticion: NSFetchRequest<T>) -> NSFetchRequest<T> {
        peticion.sortDescriptors = [NSSortDescriptor(key: "orden", ascending: true), NSSortDescriptor(key: "fechaCreacion", ascending: true)]
        return peticion
    }

    /// Nombre sugerido: `kiwu-AAAA-MM-DD.kiwu.json`.
    static func nombreDeArchivo(_ fecha: Date = .now, calendario: Calendar = .current) -> String {
        let c = calendario.dateComponents([.year, .month, .day], from: fecha)
        return String(format: "kiwu-%04d-%02d-%02d.kiwu.json", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    // MARK: Importar

    /// Importa una copia. Ignora las filas cuyo `id` ya existe (viva o eliminada) y descarta las que tienen
    /// referencias que no existen. Todo se guarda de una vez: si algo falla no queda nada a medias.
    @discardableResult
    static func importar(_ datos: Data, en contexto: NSManagedObjectContext) throws -> ResumenImportacion {
        guard let objeto = try? JSONSerialization.jsonObject(with: datos), let raiz = objeto as? [String: Any] else {
            throw ErrorCopia(mensaje: "No se pudo leer la copia: el archivo no es un JSON válido")
        }
        guard raiz["app"] as? String == "kiwu" else { throw ErrorCopia(mensaje: "El archivo no es una copia de seguridad de Kiwu") }
        if let version = (raiz["formato"] as? NSNumber)?.intValue, version > formato {
            throw ErrorCopia(mensaje: "La copia es de una versión más nueva de Kiwu")
        }

        var resumen = ResumenImportacion()
        var fallo: Error?
        contexto.performAndWait {
            do {
                resumen = try importarFilas(raiz, contexto)
                if contexto.hasChanges { try contexto.save() }
            } catch {
                contexto.rollback()
                fallo = error
            }
        }
        if let fallo { throw fallo }
        return resumen
    }

    private static func filas(_ raiz: [String: Any], _ clave: String) -> [[String: Any]] {
        (raiz[clave] as? [Any])?.compactMap { $0 as? [String: Any] } ?? []
    }

    private static func uuid(_ fila: [String: Any], _ clave: String) -> UUID? {
        (fila[clave] as? String).flatMap { UUID(uuidString: $0) }
    }

    private static func entero(_ fila: [String: Any], _ clave: String, _ defecto: Int) -> Int {
        (fila[clave] as? NSNumber).map { $0.intValue } ?? defecto
    }

    private static func ids<T: NSManagedObject>(_ peticion: NSFetchRequest<T>, _ contexto: NSManagedObjectContext) throws -> Set<UUID> {
        Set(try contexto.fetch(peticion).compactMap { $0.value(forKey: "id") as? UUID })
    }

    private static func importarFilas(_ raiz: [String: Any], _ contexto: NSManagedObjectContext) throws -> ResumenImportacion {
        var resumen = ResumenImportacion()
        let ahora = Date.now

        // Los ids existentes cuentan aunque estén eliminados: así no se resucita nada.
        var idsMaterias = try ids(Materia.fetchRequestTodas(), contexto)
        var idsDeberes = try ids(Deber.fetchRequestTodas(), contexto)
        var idsCuadernos = try ids(Cuaderno.fetchRequestTodas(), contexto)
        var idsPaginas = try ids(Pagina.fetchRequestTodas(), contexto)
        var idsHorarios = try ids(Horario.fetchRequestTodas(), contexto)
        var idsClases = try ids(Clase.fetchRequestTodas(), contexto)

        var materias = Dictionary(try contexto.fetch(Materia.fetchRequestTodas()).map { ($0.id, $0) }, uniquingKeysWith: { primero, _ in primero })
        var cuadernos = Dictionary(try contexto.fetch(Cuaderno.fetchRequestTodas()).map { ($0.id, $0) }, uniquingKeysWith: { primero, _ in primero })
        var horarios = Dictionary(try contexto.fetch(Horario.fetchRequestTodas()).map { ($0.id, $0) }, uniquingKeysWith: { primero, _ in primero })

        for f in filas(raiz, "materias") {
            guard let id = uuid(f, "id"), let nombre = f["nombre"] as? String, idsMaterias.insert(id).inserted else { continue }
            let m = Materia(context: contexto, nombre: nombre, orden: Int16(clamping: entero(f, "orden", 0)))
            m.id = id
            m.fechaCreacion = desdeIso(f["creada"]) ?? ahora
            m.fechaActualizacion = desdeIso(f["actualizada"]) ?? ahora
            materias[id] = m
            resumen.listas += 1
        }
        for f in filas(raiz, "deberes") {
            guard let id = uuid(f, "id"), let titulo = f["titulo"] as? String,
                  let idMateria = uuid(f, "materia"), let materia = materias[idMateria],
                  idsDeberes.insert(id).inserted else { continue }
            let d = Deber(context: contexto, titulo: titulo, materia: materia)
            d.id = id
            d.completado = (f["completado"] as? NSNumber)?.boolValue ?? false
            d.estado = Int16(clamping: min(max(entero(f, "estado", 0), 0), 1))
            d.fechaEntrega = desdeIso(f["fechaEntrega"])
            d.recordatorioAntesEnHoras = Int16(clamping: entero(f, "recordatorioHoras", 24))
            d.fechaCompletado = desdeIso(f["fechaCompletado"])
            d.contenidoNota = documento(f["nota"]).data
            d.fechaCreacion = desdeIso(f["creada"]) ?? ahora
            d.fechaActualizacion = desdeIso(f["actualizada"]) ?? ahora
            // Si la lista a la que apunta estaba eliminada, la tarea nace eliminada también.
            if materia.eliminada != nil { d.eliminada = materia.eliminada }
            resumen.tareas += 1
        }
        for f in filas(raiz, "cuadernos") {
            guard let id = uuid(f, "id"), let nombre = f["nombre"] as? String, idsCuadernos.insert(id).inserted else { continue }
            let c = Cuaderno(context: contexto, nombre: nombre, icono: f["icono"] as? String ?? "📓", orden: Int16(clamping: entero(f, "orden", 0)))
            c.id = id
            c.fechaCreacion = desdeIso(f["creada"]) ?? ahora
            c.fechaActualizacion = desdeIso(f["actualizada"]) ?? ahora
            cuadernos[id] = c
            resumen.cuadernos += 1
        }
        for f in filas(raiz, "paginas") {
            guard let id = uuid(f, "id"), let idCuaderno = uuid(f, "cuaderno"), let cuaderno = cuadernos[idCuaderno],
                  idsPaginas.insert(id).inserted else { continue }
            let doc = documento(f["contenido"])
            let p = Pagina(context: contexto, cuaderno: cuaderno)
            p.id = id
            p.titulo = f["titulo"] as? String ?? ""
            p.icono = f["icono"] as? String ?? ""
            p.favorita = (f["favorita"] as? NSNumber)?.boolValue ?? false
            p.contenido = doc.data
            p.textoPlano = doc.textoPlano
            p.fechaCreacion = desdeIso(f["creada"]) ?? ahora
            p.fechaActualizacion = desdeIso(f["actualizada"]) ?? ahora
            if cuaderno.eliminada != nil { p.eliminada = cuaderno.eliminada }
            resumen.notas += 1
        }
        // Horarios primero y clases después.
        for f in filas(raiz, "horarios") {
            guard let id = uuid(f, "id"), let nombre = f["nombre"] as? String, idsHorarios.insert(id).inserted else { continue }
            let h = Horario(context: contexto, nombre: nombre, icono: f["icono"] as? String ?? "🎓", orden: Int16(clamping: entero(f, "orden", 0)))
            h.id = id
            h.fechaCreacion = desdeIso(f["creada"]) ?? ahora
            h.fechaActualizacion = desdeIso(f["actualizada"]) ?? ahora
            horarios[id] = h
            resumen.horarios += 1
        }
        for f in filas(raiz, "clases") {
            let dia = entero(f, "diaSemana", 0), inicio = entero(f, "inicioMin", -1), fin = entero(f, "finMin", -1)
            guard let id = uuid(f, "id"), let titulo = f["titulo"] as? String,
                  let idHorario = uuid(f, "horario"), let horario = horarios[idHorario],
                  (1...7).contains(dia), ReglasHorario.valida(inicio: inicio, fin: fin),
                  idsClases.insert(id).inserted else { continue }
            let c = Clase(context: contexto)
            c.id = id
            c.horario = horario
            c.materiaId = uuid(f, "materia")
            c.grupo = uuid(f, "grupo") ?? id
            c.titulo = titulo
            c.diaSemana = Int16(dia)
            c.inicioMin = Int16(inicio)
            c.finMin = Int16(fin)
            c.aula = f["aula"] as? String ?? ""
            c.notas = f["notas"] as? String ?? ""
            c.aviso = (f["avisoMin"] as? NSNumber).map { $0.intValue }
            c.fechaCreacion = desdeIso(f["creada"]) ?? ahora
            c.fechaActualizacion = desdeIso(f["actualizada"]) ?? ahora
            if horario.eliminada != nil { c.eliminada = horario.eliminada }
            resumen.clases += 1
        }
        return resumen
    }
}

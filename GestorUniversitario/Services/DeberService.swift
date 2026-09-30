import CoreData

@MainActor
struct MateriaService {
    let persistence: PersistenceController
    let reminders: ReminderService
    private let finder = FinderService()

    /// Vincula la materia con una carpeta del Finder (se guarda una referencia; los archivos no se copian).
    func vincularCarpeta(_ materia: Materia, _ url: URL) {
        do {
            materia.bookmarkCarpeta = try finder.makeBookmark(for: url)
            materia.carpetaDisponible = true
            materia.fechaActualizacion = .now
            persistence.save()
        } catch {
            NSLog("No se pudo vincular la carpeta \(url.lastPathComponent): \(error.localizedDescription)")
        }
    }

    func desvincularCarpeta(_ materia: Materia) {
        materia.bookmarkCarpeta = nil
        materia.carpetaDisponible = false
        materia.fechaActualizacion = .now
        persistence.save()
    }

    /// Comprueba que la carpeta vinculada sigue existiendo y actualiza el aviso de la materia.
    func comprobarCarpeta(_ materia: Materia) {
        guard let data = materia.bookmarkCarpeta else {
            if materia.carpetaDisponible {
                materia.carpetaDisponible = false
                persistence.save()
            }
            return
        }
        var disponible = false
        var marcadorNuevo: Data?
        if let resuelto = try? finder.resolveBookmark(data) {
            let acceso = resuelto.url.startAccessingSecurityScopedResource()
            var esCarpeta: ObjCBool = false
            disponible = FileManager.default.fileExists(atPath: resuelto.url.path, isDirectory: &esCarpeta) && esCarpeta.boolValue
            if disponible, resuelto.isStale { marcadorNuevo = try? finder.makeBookmark(for: resuelto.url) }
            if acceso { resuelto.url.stopAccessingSecurityScopedResource() }
        }
        guard disponible != materia.carpetaDisponible || marcadorNuevo != nil else { return }
        materia.carpetaDisponible = disponible
        if let marcadorNuevo { materia.bookmarkCarpeta = marcadorNuevo }
        persistence.save()
    }

    func comprobarTodas(_ materias: [Materia]) {
        materias.forEach(comprobarCarpeta)
    }

    @discardableResult
    func crear(nombre: String) -> Materia {
        let materia = Materia(context: persistence.container.viewContext, nombre: nombre, orden: Int16(clamping: Int(Date.now.timeIntervalSince1970) % 30000))
        persistence.save()
        return materia
    }

    func renombrar(_ materia: Materia, a nombre: String) {
        materia.nombre = nombre
        materia.fechaActualizacion = .now
        persistence.save()
    }

    func eliminar(_ materia: Materia) {
        materia.deberes.forEach { reminders.cancel(for: $0) }
        persistence.container.viewContext.delete(materia)
        persistence.save()
    }
}

@MainActor
struct DeberService {
    let persistence: PersistenceController
    let reminders: ReminderService

    @discardableResult
    func crear(titulo: String, en materia: Materia, fecha: Date? = nil) -> Deber {
        let deber = Deber(context: persistence.container.viewContext, titulo: titulo, materia: materia)
        deber.fechaEntrega = fecha
        persistence.save()
        if fecha != nil { Task { await reminders.reprogramar(deber) } }
        return deber
    }

    func mover(_ deber: Deber, a materia: Materia) {
        deber.materia = materia
        guardar(deber)
    }

    /// Guarda cambios de un deber y sincroniza su recordatorio.
    func guardar(_ deber: Deber, reprogramar: Bool = true) {
        guard !deber.isDeleted, deber.managedObjectContext != nil else { return }
        deber.fechaActualizacion = .now
        persistence.save()
        if reprogramar { Task { await reminders.reprogramar(deber) } }
    }

    func marcar(_ deber: Deber, completado: Bool) {
        deber.completado = completado
        deber.fechaCompletado = completado ? .now : nil
        guardar(deber)
    }

    func eliminar(_ deber: Deber) {
        reminders.cancel(for: deber)
        persistence.container.viewContext.delete(deber)
        persistence.save()
    }
}

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
        // Al final de la lista: mayor orden existente + 1 (igual que en Linux; antes se derivaba del reloj y podía repetirse).
        let contexto = persistence.container.viewContext
        let mayorOrden = ((try? contexto.fetch(Materia.fetchRequest())) ?? []).map { Int($0.orden) }.max() ?? -1
        let materia = Materia(context: contexto, nombre: nombre, orden: Int16(clamping: mayorOrden + 1))
        persistence.save()
        return materia
    }

    func renombrar(_ materia: Materia, a nombre: String) {
        materia.nombre = nombre
        materia.fechaActualizacion = .now
        persistence.save()
    }

    func eliminar(_ materia: Materia) {
        // Borrado lógico: se marca la lista y todas sus tareas (así se puede sincronizar y recuperar).
        let ahora = Date.now
        for deber in materia.deberes where deber.eliminada == nil {
            reminders.cancel(for: deber)
            deber.eliminada = ahora
            deber.fechaActualizacion = ahora
        }
        materia.eliminada = ahora
        materia.fechaActualizacion = ahora
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

    /// Mueve la tarea a una columna del flujo (Por hacer, En curso, Hecho).
    func mover(_ deber: Deber, a columna: ColumnaFlujo) {
        guard deber.columna != columna else { return }
        switch columna {
        case .porHacer, .enCurso:
            deber.estado = columna.rawValue
            deber.completado = false
            deber.fechaCompletado = nil
        case .hecho:
            deber.completado = true
            deber.fechaCompletado = .now
        }
        guardar(deber)
    }

    func marcar(_ deber: Deber, completado: Bool) {
        deber.completado = completado
        if !completado { deber.estado = deber.estado == ColumnaFlujo.enCurso.rawValue ? deber.estado : 0 }
        deber.fechaCompletado = completado ? .now : nil
        guardar(deber)
    }

    func eliminar(_ deber: Deber) {
        reminders.cancel(for: deber)
        deber.eliminada = .now
        deber.fechaActualizacion = .now
        persistence.save()
    }
}

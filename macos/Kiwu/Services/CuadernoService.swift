import CoreData

struct CuadernoService {
    let persistence: PersistenceController

    @discardableResult
    func crearCuaderno(nombre: String, icono: String, orden: Int) -> Cuaderno {
        let cuaderno = Cuaderno(context: persistence.container.viewContext, nombre: nombre, icono: icono, orden: Int16(clamping: orden))
        persistence.save()
        return cuaderno
    }

    /// Primer cuaderno; si todavía no existe ninguno, crea "Notas rápidas".
    func primerCuaderno() -> Cuaderno {
        let peticion = Cuaderno.fetchRequest()
        peticion.sortDescriptors = [NSSortDescriptor(key: "orden", ascending: true), NSSortDescriptor(key: "nombre", ascending: true)]
        peticion.fetchLimit = 1
        if let existente = try? persistence.container.viewContext.fetch(peticion).first { return existente }
        return crearCuaderno(nombre: "Notas rápidas", icono: "📝", orden: 0)
    }

    func cuaderno(id: UUID) -> Cuaderno? {
        let peticion = Cuaderno.fetchRequest()
        peticion.predicate = NSPredicate(format: "id == %@ AND eliminada == nil", id as CVarArg)
        peticion.fetchLimit = 1
        return try? persistence.container.viewContext.fetch(peticion).first
    }

    func editar(_ cuaderno: Cuaderno, nombre: String, icono: String) {
        cuaderno.nombre = nombre
        cuaderno.icono = icono
        cuaderno.fechaActualizacion = .now
        persistence.save()
    }

    func eliminar(_ cuaderno: Cuaderno) {
        // Borrado lógico: se marca el cuaderno y todas sus páginas.
        let ahora = Date.now
        for pagina in cuaderno.paginas where pagina.eliminada == nil {
            pagina.eliminada = ahora
            pagina.fechaActualizacion = ahora
        }
        cuaderno.eliminada = ahora
        cuaderno.fechaActualizacion = ahora
        persistence.save()
    }

    @discardableResult
    func crearPagina(en cuaderno: Cuaderno) -> Pagina {
        let pagina = Pagina(context: persistence.container.viewContext, cuaderno: cuaderno)
        persistence.save()
        return pagina
    }

    /// Nota más reciente del cuaderno que sigue completamente en blanco (sin título, texto ni adjuntos).
    func paginaEnBlanco(en cuaderno: Cuaderno) -> Pagina? {
        let peticion = Pagina.fetchRequest()
        peticion.predicate = NSPredicate(format: "cuaderno == %@ AND eliminada == nil AND titulo == '' AND icono == ''", cuaderno)
        peticion.sortDescriptors = [NSSortDescriptor(key: "fechaCreacion", ascending: false)]
        peticion.fetchLimit = 5
        let candidatas = (try? persistence.container.viewContext.fetch(peticion)) ?? []
        return candidatas.first { pagina in
            NotaService.leer(pagina.contenido).blocks.allSatisfy { $0.kind == .paragraph && $0.text.isEmpty }
        }
    }

    func guardar(_ pagina: Pagina) {
        guard !pagina.estaEliminado else { return }
        pagina.fechaActualizacion = .now
        persistence.save()
    }

    func alternarFavorita(_ pagina: Pagina) {
        pagina.favorita.toggle()
        guardar(pagina)
    }

    func mover(_ pagina: Pagina, a cuaderno: Cuaderno) {
        pagina.cuaderno = cuaderno
        guardar(pagina)
    }

    func eliminar(_ pagina: Pagina) {
        pagina.eliminada = .now
        pagina.fechaActualizacion = .now
        persistence.save()
    }
}

import CoreData

/// Lo que el formulario de clase entrega al guardar: una clase (con grupo) en uno o varios días.
struct BorradorClase {
    var grupo: UUID?
    var horario: Horario
    var materiaId: UUID?
    var titulo: String
    var dias: Set<Int>
    var inicioMin: Int
    var finMin: Int
    var aula = ""
    var notas = ""
    var avisoMin: Int?
}

@MainActor
struct HorarioService {
    let persistence: PersistenceController
    private var contexto: NSManagedObjectContext { persistence.container.viewContext }

    @discardableResult
    func crearHorario(nombre: String, icono: String = "🎓") -> Horario {
        let mayor = ((try? contexto.fetch(Horario.fetchRequest())) ?? []).map { Int($0.orden) }.max() ?? -1
        let horario = Horario(context: contexto, nombre: nombre, icono: icono, orden: Int16(clamping: mayor + 1))
        persistence.save()
        return horario
    }

    func editar(_ horario: Horario, nombre: String, icono: String) {
        horario.nombre = nombre
        horario.icono = icono
        horario.fechaActualizacion = .now
        persistence.save()
    }

    /// Borrado lógico: se marca el horario y todas sus clases.
    func eliminar(_ horario: Horario) {
        let ahora = Date.now
        for clase in horario.clases where clase.eliminada == nil {
            clase.eliminada = ahora
            clase.fechaActualizacion = ahora
        }
        horario.eliminada = ahora
        horario.fechaActualizacion = ahora
        persistence.save()
    }

    /// Crea o actualiza una clase y sus días. Es como `guardarClase` de Android: las filas de los días que se
    /// mantienen conservan su fecha de creación, los días nuevos crean filas y los días quitados se marcan eliminados.
    /// Devuelve el grupo guardado, o nil si las horas no son válidas.
    @discardableResult
    func guardarClase(_ borrador: BorradorClase) -> UUID? {
        guard ReglasHorario.valida(inicio: borrador.inicioMin, fin: borrador.finMin) else { return nil }
        let grupo = borrador.grupo ?? UUID()
        let ahora = Date.now
        let existentes = ((try? contexto.fetch(Clase.fetchRequest())) ?? []).filter { $0.grupo == grupo }
        let titulo = borrador.titulo.trimmingCharacters(in: .whitespacesAndNewlines)

        for dia in borrador.dias.filter({ (1...7).contains($0) }).sorted() {
            let clase = existentes.first { Int($0.diaSemana) == dia } ?? {
                let nueva = Clase(context: contexto)
                nueva.id = UUID()
                nueva.grupo = grupo
                nueva.diaSemana = Int16(dia)
                nueva.fechaCreacion = ahora
                return nueva
            }()
            clase.horario = borrador.horario
            clase.materiaId = borrador.materiaId
            clase.titulo = titulo
            clase.inicioMin = Int16(borrador.inicioMin)
            clase.finMin = Int16(borrador.finMin)
            clase.aula = borrador.aula
            clase.notas = borrador.notas
            clase.aviso = borrador.avisoMin
            clase.fechaActualizacion = ahora
        }
        for quitada in existentes where !borrador.dias.contains(Int(quitada.diaSemana)) {
            quitada.eliminada = ahora
            quitada.fechaActualizacion = ahora
        }
        persistence.save()
        return grupo
    }

    /// Borra (de forma lógica) todas las filas del grupo: la clase en todos sus días.
    func eliminarGrupo(_ grupo: UUID) {
        let ahora = Date.now
        for clase in ((try? contexto.fetch(Clase.fetchRequest())) ?? []) where clase.grupo == grupo {
            clase.eliminada = ahora
            clase.fechaActualizacion = ahora
        }
        persistence.save()
    }
}

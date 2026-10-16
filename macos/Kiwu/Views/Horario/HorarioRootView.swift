import SwiftUI
import CoreData
import Combine

enum HorarioSeleccion: Hashable {
    case todos
    case horario(UUID)
}

private enum VistaHorario: String, CaseIterable, Identifiable {
    case semana = "Semana"
    case dia = "Día"
    var id: String { rawValue }
}

private enum HojaHorario: Identifiable {
    case nuevo
    case editar(Horario)
    case clase(Clase?, dia: Int)

    var id: String {
        switch self {
        case .nuevo: return "nuevo"
        case .editar(let h): return "editar-\(h.id)"
        case .clase(let c, let dia): return "clase-\(c?.id.uuidString ?? "nueva")-\(dia)"
        }
    }
}

/// Módulo Horario: barra lateral de horarios y, a la derecha, «Ahora / Siguiente» con la vista de semana o de día.
struct HorarioRootView: View {
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Horario.orden, ascending: true), NSSortDescriptor(keyPath: \Horario.nombre, ascending: true)],
        predicate: NSPredicate(format: "eliminada == nil"),
        animation: .default
    ) private var horarios: FetchedResults<Horario>

    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Clase.inicioMin, ascending: true)],
        predicate: NSPredicate(format: "eliminada == nil AND horario.eliminada == nil"),
        animation: .default
    ) private var todasLasClases: FetchedResults<Clase>

    @Environment(\.managedObjectContext) private var context
    @State private var seleccion: HorarioSeleccion = .todos
    @State private var vista: VistaHorario = .semana
    @State private var diaElegido = ReglasHorario.diaDe(.now)
    @State private var hoja: HojaHorario?
    @State private var horarioAEliminar: Horario?
    @State private var ahora = Date.now

    private let service = HorarioService(persistence: .shared)
    private let reloj = Timer.publish(every: 30, on: .main, in: .common).autoconnect()

    private var clases: [Clase] {
        switch seleccion {
        case .todos: return Array(todasLasClases)
        case .horario(let id): return todasLasClases.filter { $0.horario.id == id }
        }
    }

    private var horarioActual: Horario? {
        if case .horario(let id) = seleccion { return horarios.first { $0.id == id } }
        return nil
    }

    var body: some View {
        HStack(spacing: 0) {
            lateral.frame(width: 230)
            Divider()
            contenido
        }
        .onReceive(reloj) { ahora = $0 }
        .onAppear { Task { await AvisosHorario.reprogramar(en: context) } }
        .sheet(item: $hoja) { hoja in
            switch hoja {
            case .nuevo:
                HorarioFormView(titulo: "Nuevo horario", nombre: "", emoji: "🎓", sugerencias: true) { nombre, emoji in
                    let nuevo = service.crearHorario(nombre: nombre, icono: emoji)
                    seleccion = .horario(nuevo.id)
                }
            case .editar(let horario):
                HorarioFormView(titulo: "Editar horario", nombre: horario.nombre, emoji: horario.icono, sugerencias: false) { nombre, emoji in
                    service.editar(horario, nombre: nombre, icono: emoji)
                }
            case .clase(let clase, let dia):
                ClaseFormView(clase: clase, horarios: Array(horarios), horarioInicial: horarioActual ?? horarios.first,
                              diaInicial: dia, existentes: Array(todasLasClases), service: service) {
                    Task { await AvisosHorario.reprogramar(en: context) }
                }
            }
        }
        .confirmationDialog("¿Eliminar este horario?", isPresented: Binding(get: { horarioAEliminar != nil }, set: { if !$0 { horarioAEliminar = nil } }), presenting: horarioAEliminar) { horario in
            Button("Eliminar «\(horario.nombre)» y sus clases", role: .destructive) {
                service.eliminar(horario)
                if seleccion == .horario(horario.id) { seleccion = .todos }
                Task { await AvisosHorario.reprogramar(en: context) }
            }
        } message: { _ in
            Text("Se eliminarán también todas sus clases.")
        }
    }

    // MARK: Barra lateral

    private var lateral: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 2) {
                SidebarFila(activo: seleccion == .todos, icono: .simbolo("calendar.day.timeline.left"), titulo: "Todos los horarios", cuenta: todasLasClases.count) {
                    seleccion = .todos
                }
                HStack {
                    Text("Horarios").font(.subheadline).foregroundStyle(.secondary)
                    Spacer()
                    Button { hoja = .nuevo } label: { Image(systemName: "plus") }
                        .buttonStyle(.plain)
                        .foregroundStyle(.secondary)
                        .help("Nuevo horario")
                }
                .padding(.horizontal, 10)
                .padding(.top, 18)
                .padding(.bottom, 4)

                ForEach(horarios) { horario in
                    SidebarFila(activo: seleccion == .horario(horario.id), icono: .emoji(horario.icono), titulo: horario.nombre,
                                cuenta: todasLasClases.filter { $0.horario.id == horario.id }.count) {
                        seleccion = .horario(horario.id)
                    }
                    .contextMenu {
                        Button("Editar horario") { hoja = .editar(horario) }
                        Button("Eliminar horario", role: .destructive) { horarioAEliminar = horario }
                    }
                }
            }
            .padding(10)
        }
        .background(Color.primary.opacity(0.04))
    }

    // MARK: Contenido

    @ViewBuilder
    private var contenido: some View {
        if horarios.isEmpty {
            EmptyStateView(titulo: "Crea tu primer horario", icono: "calendar.day.timeline.left",
                           botonTitulo: "Nuevo horario") { hoja = .nuevo }
        } else {
            VStack(spacing: 0) {
                cabecera
                Divider()
                Group {
                    if vista == .semana {
                        SemanaHorarioView(clases: clases, ahora: ahora, mostrarHorario: seleccion == .todos,
                                          onNueva: { hoja = .clase(nil, dia: $0) }, onEditar: { hoja = .clase($0, dia: Int($0.diaSemana)) })
                    } else {
                        DiaHorarioView(clases: clases, dia: $diaElegido, ahora: ahora, mostrarHorario: seleccion == .todos,
                                       onNueva: { hoja = .clase(nil, dia: $0) }, onEditar: { hoja = .clase($0, dia: Int($0.diaSemana)) })
                    }
                }
            }
        }
    }

    private var cabecera: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(horarioActual.map { "\($0.icono) \($0.nombre)" } ?? "Todos los horarios").font(.title2.bold())
                Spacer()
                Picker("", selection: $vista) {
                    ForEach(VistaHorario.allCases) { Text($0.rawValue).tag($0) }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
                .frame(width: 160)
                Button {
                    hoja = .clase(nil, dia: ReglasHorario.diaDe(.now))
                } label: { Label("Nueva clase", systemImage: "plus") }
                    .buttonStyle(.borderedProminent)
            }
            tarjetaAhora
        }
        .padding(16)
    }

    private var tarjetaAhora: some View {
        let datos = clases.map(\.datos)
        let dia = ReglasHorario.diaDe(ahora)
        let minuto = ReglasHorario.minutoDe(ahora)
        let enCurso = ReglasHorario.enCurso(datos, dia: dia, minuto: minuto)
        let siguiente = ReglasHorario.siguiente(datos, dia: dia, minuto: minuto)
        return VStack(alignment: .leading, spacing: 4) {
            if let enCurso {
                Text("Ahora: \(enCurso.titulo) · hasta \(ReglasHorario.hora(enCurso.finMin))").font(.headline)
            } else {
                Text("Ahora: nada en curso").font(.headline).foregroundStyle(.secondary)
            }
            if let siguiente {
                Text("Siguiente: \(siguiente.clase.titulo) · \(ReglasHorario.cuando(dias: siguiente.dias, dia: siguiente.clase.diaSemana)) \(ReglasHorario.hora(siguiente.clase.inicioMin))")
                    .font(.subheadline).foregroundStyle(.secondary)
            } else {
                Text("Siguiente: no hay clases próximas").font(.subheadline).foregroundStyle(.secondary)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 10).fill(Color.accentColor.opacity(0.12)))
    }
}

// MARK: Color de una clase

/// El color de una clase no se guarda: sale del pastel estable de su lista de tareas (o de su grupo).
private func pastel(_ clase: Clase) -> ColorPastel {
    let id = clase.materiaId ?? clase.grupo
    return PaletaPastel.asignar(ids: [id])[id] ?? PaletaPastel.colores[0]
}

// MARK: Vista de semana

private struct SemanaHorarioView: View {
    let clases: [Clase]
    let ahora: Date
    let mostrarHorario: Bool
    let onNueva: (Int) -> Void
    let onEditar: (Clase) -> Void

    private let alturaHora: CGFloat = 52
    private let anchoEje: CGFloat = 46

    /// Rango visible: de min(07:00, primera clase) a max(19:00, última clase), recortado a 0–24.
    private var rango: (desde: Int, hasta: Int) {
        let primera = clases.map { Int($0.inicioMin) / 60 }.min() ?? 7
        let ultima = clases.map { (Int($0.finMin) + 59) / 60 }.max() ?? 19
        return (min(max(min(7, primera), 0), 24), max(min(max(19, ultima), 24), 1))
    }

    var body: some View {
        let (desde, hasta) = rango
        let alto = CGFloat(hasta - desde) * alturaHora
        ScrollView {
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    Color.clear.frame(width: anchoEje)
                    ForEach(1...7, id: \.self) { dia in
                        Button { onNueva(dia) } label: {
                            Text(ReglasHorario.diasCortos[dia - 1])
                                .font(.subheadline.weight(dia == ReglasHorario.diaDe(ahora) ? .bold : .regular))
                                .foregroundStyle(dia == ReglasHorario.diaDe(ahora) ? Color.accentColor : .secondary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                        }
                        .buttonStyle(.plain)
                        .help("Añadir una clase el \(ReglasHorario.nombreDia(dia).lowercased())")
                    }
                }
                HStack(alignment: .top, spacing: 0) {
                    ZStack(alignment: .topTrailing) {
                        ForEach(desde..<hasta, id: \.self) { h in
                            Text(ReglasHorario.hora(h * 60))
                                .font(.caption2).foregroundStyle(.secondary)
                                .offset(y: CGFloat(h - desde) * alturaHora - 6)
                                .padding(.trailing, 6)
                        }
                    }
                    .frame(width: anchoEje, height: alto, alignment: .topTrailing)

                    ForEach(1...7, id: \.self) { dia in
                        columna(dia: dia, desde: desde, hasta: hasta, alto: alto)
                    }
                }
                .padding(.bottom, 12)
            }
        }
    }

    private func columna(dia: Int, desde: Int, hasta: Int, alto: CGFloat) -> some View {
        let delDia = clases.filter { Int($0.diaSemana) == dia }
        let carriles = Self.carriles(delDia)
        return GeometryReader { geo in
            ZStack(alignment: .topLeading) {
                ForEach(desde..<hasta, id: \.self) { h in
                    Rectangle().fill(Color.primary.opacity(0.08)).frame(height: 1)
                        .offset(y: CGFloat(h - desde) * alturaHora)
                }
                ForEach(delDia) { clase in
                    let (carril, total) = carriles[clase.id] ?? (0, 1)
                    let ancho = geo.size.width / CGFloat(total)
                    let inicio = max(Int(clase.inicioMin), desde * 60)
                    let fin = min(Int(clase.finMin), hasta * 60)
                    let alturaBloque = max(CGFloat(fin - inicio) / 60 * alturaHora - 2, 18)
                    bloque(clase)
                        .frame(width: ancho - 3, height: alturaBloque, alignment: .topLeading)
                        .offset(x: CGFloat(carril) * ancho + 1, y: CGFloat(inicio - desde * 60) / 60 * alturaHora + 1)
                }
            }
        }
        .frame(height: alto)
        .frame(maxWidth: .infinity)
        .overlay(alignment: .leading) { Rectangle().fill(Color.primary.opacity(0.08)).frame(width: 1) }
    }

    private func bloque(_ clase: Clase) -> some View {
        let color = pastel(clase)
        return Button { onEditar(clase) } label: {
            VStack(alignment: .leading, spacing: 1) {
                Text(clase.titulo).font(.caption.weight(.semibold)).lineLimit(2)
                Text(ReglasHorario.rango(clase.datos)).font(.caption2).lineLimit(1)
                if !clase.aula.isEmpty { Text(clase.aula).font(.caption2).lineLimit(1) }
                if mostrarHorario { Text(clase.horario.nombre).font(.caption2).lineLimit(1) }
            }
            .foregroundStyle(color.texto)
            .padding(4)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(RoundedRectangle(cornerRadius: 6).fill(color.fondo))
            .clipShape(RoundedRectangle(cornerRadius: 6))
        }
        .buttonStyle(.plain)
    }

    /// Reparte las clases que coinciden en carriles: devuelve (carril, nº de carriles del grupo que se solapa).
    static func carriles(_ clases: [Clase]) -> [UUID: (Int, Int)] {
        let ordenadas = clases.sorted { ($0.inicioMin, $0.finMin) < ($1.inicioMin, $1.finMin) }
        var resultado: [UUID: (Int, Int)] = [:]
        var grupo: [(Clase, Int)] = []
        var finGrupo = 0
        func cerrar() {
            let total = (grupo.map(\.1).max() ?? 0) + 1
            for (c, carril) in grupo { resultado[c.id] = (carril, total) }
            grupo = []
        }
        for clase in ordenadas {
            if !grupo.isEmpty && Int(clase.inicioMin) >= finGrupo { cerrar() }
            var carril = 0
            while grupo.contains(where: { $0.1 == carril && Int($0.0.finMin) > Int(clase.inicioMin) }) { carril += 1 }
            grupo.append((clase, carril))
            finGrupo = max(finGrupo, Int(clase.finMin))
        }
        cerrar()
        return resultado
    }
}

// MARK: Vista por día

private struct DiaHorarioView: View {
    let clases: [Clase]
    @Binding var dia: Int
    let ahora: Date
    let mostrarHorario: Bool
    let onNueva: (Int) -> Void
    let onEditar: (Clase) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 6) {
                ForEach(1...7, id: \.self) { d in
                    Button { dia = d } label: {
                        Text(ReglasHorario.diasLetra[d - 1])
                            .font(.subheadline.weight(.semibold))
                            .frame(width: 34, height: 34)
                            .foregroundStyle(dia == d ? Color.white : (d == ReglasHorario.diaDe(ahora) ? Color.accentColor : .primary))
                            .background(Circle().fill(dia == d ? Color.accentColor : Color.primary.opacity(0.06)))
                    }
                    .buttonStyle(.plain)
                    .help(ReglasHorario.nombreDia(d))
                }
                Spacer()
                Text(ReglasHorario.nombreDia(dia)).font(.headline)
            }
            .padding(16)

            let delDia = clases.filter { Int($0.diaSemana) == dia }.sorted { ($0.inicioMin, $0.finMin, $0.titulo) < ($1.inicioMin, $1.finMin, $1.titulo) }
            if delDia.isEmpty {
                EmptyStateView(titulo: "Sin clases el \(ReglasHorario.nombreDia(dia).lowercased())", icono: "calendar",
                               botonTitulo: "Añadir clase") { onNueva(dia) }
            } else {
                ScrollView {
                    VStack(spacing: 8) {
                        ForEach(delDia) { clase in
                            let color = pastel(clase)
                            Button { onEditar(clase) } label: {
                                HStack(alignment: .top, spacing: 14) {
                                    VStack(alignment: .trailing) {
                                        Text(ReglasHorario.hora(Int(clase.inicioMin))).font(.headline)
                                        Text(ReglasHorario.hora(Int(clase.finMin))).font(.subheadline).opacity(0.7)
                                    }
                                    .monospacedDigit()
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(clase.titulo).font(.headline)
                                        if !clase.aula.isEmpty { Text(clase.aula).font(.subheadline) }
                                        if mostrarHorario { Text(clase.horario.nombre).font(.caption).opacity(0.7) }
                                    }
                                    Spacer()
                                }
                                .foregroundStyle(color.texto)
                                .padding(12)
                                .background(RoundedRectangle(cornerRadius: 10).fill(color.fondo))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
            }
        }
    }
}

// MARK: Formulario de horario

private struct HorarioFormView: View {
    @Environment(\.dismiss) private var dismiss
    let titulo: String
    @State var nombre: String
    @State var emoji: String
    let sugerencias: Bool
    let onGuardar: (String, String) -> Void

    private var limpio: String { nombre.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(titulo).font(.title2.bold())
            HStack(spacing: 10) {
                Text(emoji).font(.system(size: 28))
                TextField("Nombre del horario", text: $nombre).onSubmit(guardar)
            }
            if sugerencias {
                HStack {
                    ForEach([("Universidad", "🎓"), ("Trabajo", "💼"), ("Ocio", "🌴")], id: \.0) { s in
                        Button("\(s.1) \(s.0)") { nombre = s.0; emoji = s.1 }
                    }
                }
            }
            EmojiPicker(onElegir: { emoji = $0 })
                .background(RoundedRectangle(cornerRadius: 8).fill(Color.primary.opacity(0.05)))
            HStack {
                Spacer()
                Button("Cancelar") { dismiss() }
                Button("Guardar", action: guardar).keyboardShortcut(.defaultAction).disabled(limpio.isEmpty)
            }
        }
        .padding(24)
        .frame(width: 400)
    }

    private func guardar() {
        guard !limpio.isEmpty else { return }
        onGuardar(limpio, emoji)
        dismiss()
    }
}

// MARK: Formulario de clase

private struct ClaseFormView: View {
    @Environment(\.dismiss) private var dismiss
    let clase: Clase?
    let horarios: [Horario]
    let existentes: [Clase]
    let service: HorarioService
    let alGuardar: () -> Void

    @State private var titulo: String
    @State private var materiaId: UUID?
    @State private var horario: Horario
    @State private var dias: Set<Int>
    @State private var inicio: Int
    @State private var fin: Int
    @State private var aula: String
    @State private var notas: String
    @State private var aviso: Int
    @State private var confirmandoBorrado = false
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \Materia.orden, ascending: true)], predicate: NSPredicate(format: "eliminada == nil"))
    private var materias: FetchedResults<Materia>

    private let grupo: UUID?
    /// 0 = sin aviso.
    private static let opcionesAviso = [0] + ReglasHorario.avisos

    init(clase: Clase?, horarios: [Horario], horarioInicial: Horario?, diaInicial: Int, existentes: [Clase],
         service: HorarioService, alGuardar: @escaping () -> Void) {
        self.clase = clase
        self.horarios = horarios
        self.existentes = existentes
        self.service = service
        self.alGuardar = alGuardar
        grupo = clase?.grupo
        let delGrupo = clase.map { c in existentes.filter { $0.grupo == c.grupo } } ?? []
        _titulo = State(initialValue: clase?.titulo ?? "")
        _materiaId = State(initialValue: clase?.materiaId)
        _horario = State(initialValue: clase?.horario ?? horarioInicial ?? horarios[0])
        _dias = State(initialValue: delGrupo.isEmpty ? [diaInicial] : Set(delGrupo.map { Int($0.diaSemana) }))
        _inicio = State(initialValue: clase.map { Int($0.inicioMin) } ?? 480)
        _fin = State(initialValue: clase.map { Int($0.finMin) } ?? 600)
        _aula = State(initialValue: clase?.aula ?? "")
        _notas = State(initialValue: clase?.notas ?? "")
        _aviso = State(initialValue: clase?.aviso ?? 0)
    }

    private var tituloLimpio: String { titulo.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var horasValidas: Bool { ReglasHorario.valida(inicio: inicio, fin: fin) }

    private var choques: [Clase] {
        guard horasValidas else { return [] }
        let datos = existentes.map(\.datos)
        let ids = Set(ReglasHorario.choques(existentes: datos, grupo: grupo, dias: dias, inicio: inicio, fin: fin).map(\.id))
        return existentes.filter { ids.contains($0.id) }.sorted { ($0.diaSemana, $0.inicioMin) < ($1.diaSemana, $1.inicioMin) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(clase == nil ? "Nueva clase" : "Editar clase").font(.title2.bold())

            HStack {
                TextField("Materia o actividad", text: $titulo)
                    .onChange(of: titulo) { nuevo in
                        // Si se cambia el texto, deja de estar vinculada a la lista de tareas.
                        if let id = materiaId, materias.first(where: { $0.id == id })?.nombre != nuevo { materiaId = nil }
                    }
                if !materias.isEmpty {
                    Menu("Mis listas") {
                        ForEach(materias) { materia in
                            Button(materia.nombre) { titulo = materia.nombre; materiaId = materia.id }
                        }
                    }
                    .fixedSize()
                }
            }
            if materiaId != nil {
                Text("Vinculada a una lista de tareas").font(.caption).foregroundStyle(.secondary)
            }

            if horarios.count > 1 {
                Picker("Horario", selection: $horario) {
                    ForEach(horarios) { Text("\($0.icono) \($0.nombre)").tag($0) }
                }
            }

            VStack(alignment: .leading, spacing: 6) {
                Text("Días").font(.subheadline).foregroundStyle(.secondary)
                HStack(spacing: 6) {
                    ForEach(1...7, id: \.self) { d in
                        Toggle(ReglasHorario.diasLetra[d - 1], isOn: Binding(
                            get: { dias.contains(d) },
                            set: { if $0 { dias.insert(d) } else { dias.remove(d) } }
                        ))
                        .toggleStyle(.button)
                        .help(ReglasHorario.nombreDia(d))
                    }
                }
            }

            HStack(spacing: 16) {
                SelectorHora(titulo: "Inicio", minutos: $inicio, esFin: false)
                SelectorHora(titulo: "Fin", minutos: $fin, esFin: true)
            }
            if !horasValidas {
                Text("La hora de fin debe ser posterior a la de inicio").font(.caption).foregroundStyle(.red)
            }

            TextField("Aula", text: $aula)
            Picker("Aviso", selection: $aviso) {
                ForEach(Self.opcionesAviso, id: \.self) { Text($0 == 0 ? "Sin aviso" : "\($0) min antes").tag($0) }
            }
            TextField("Notas", text: $notas, axis: .vertical).lineLimit(2...4)

            if !choques.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(choques) { c in
                        Text("Coincide con otra clase: \(c.titulo) · \(ReglasHorario.diasCortos[Int(c.diaSemana) - 1]) \(ReglasHorario.rango(c.datos)) (\(c.horario.nombre))")
                    }
                }
                .font(.caption)
                .foregroundStyle(.orange)
            }

            HStack {
                if clase != nil {
                    Button("Eliminar", role: .destructive) { confirmandoBorrado = true }
                }
                Spacer()
                Button("Cancelar") { dismiss() }
                Button("Guardar", action: guardar)
                    .keyboardShortcut(.defaultAction)
                    .disabled(tituloLimpio.isEmpty || dias.isEmpty || !horasValidas)
            }
        }
        .padding(24)
        .frame(width: 460)
        .confirmationDialog("¿Eliminar esta clase en todos sus días?", isPresented: $confirmandoBorrado) {
            Button("Eliminar", role: .destructive) {
                if let g = grupo { service.eliminarGrupo(g) }
                alGuardar()
                dismiss()
            }
        }
    }

    private func guardar() {
        service.guardarClase(BorradorClase(grupo: grupo, horario: horario, materiaId: materiaId, titulo: tituloLimpio, dias: dias,
                                           inicioMin: inicio, finMin: fin, aula: aula, notas: notas, avisoMin: aviso == 0 ? nil : aviso))
        alGuardar()
        dismiss()
    }
}

// MARK: Selector de hora

/// Hora y minutos por separado (horas 1–12 con a. m./p. m., minutos 00–59), que se escriben o suben y bajan con las flechas.
/// Guarda minutos desde las 00:00; en el fin, 12:00 a. m. es la medianoche del final del día (1440).
private struct SelectorHora: View {
    let titulo: String
    @Binding var minutos: Int
    let esFin: Bool

    private var fecha: Binding<Date> {
        Binding(
            get: { ReglasHorario.fecha(deMinutos: minutos) },
            set: { minutos = ReglasHorario.minutos(de: $0, esFin: esFin) }
        )
    }

    var body: some View {
        HStack(spacing: 8) {
            Text(titulo)
            DatePicker(titulo, selection: fecha, displayedComponents: .hourAndMinute)
                .labelsHidden()
                .datePickerStyle(.stepperField)
                // es_US usa 12 horas con a. m./p. m. aunque el Mac esté en formato de 24 h.
                .environment(\.locale, Locale(identifier: "es_US"))
                .fixedSize()
        }
    }
}

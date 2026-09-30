import SwiftUI

struct TaskDetailView: View {
    @ObservedObject var deber: Deber
    let materias: [Materia]
    let service: DeberService
    let onDelete: () -> Void

    @StateObject private var model: NoteEditorModel
    @State private var mostrandoFecha = false

    init(deber: Deber, materias: [Materia], service: DeberService, onDelete: @escaping () -> Void) {
        self.deber = deber
        self.materias = materias
        self.service = service
        self.onDelete = onDelete
        _model = StateObject(wrappedValue: NoteEditorModel(deber: deber))
    }

    var body: some View {
        if deber.estaEliminado {
            Color.clear
        } else {
            VStack(spacing: 0) {
                barraSuperior
                Divider()
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        TextField("Título", text: Binding(
                            get: { deber.titulo },
                            set: { deber.titulo = $0; service.guardar(deber, reprogramar: false) }
                        ))
                        .textFieldStyle(.plain)
                        .font(.system(size: 24, weight: .semibold))
                        .onSubmit { service.guardar(deber) }

                        NoteEditorView(model: model)
                    }
                    .padding(.horizontal, 28)
                    .padding(.vertical, 22)
                }
                Divider()
                pie
            }
            .onDisappear {
                model.guardar()
                service.guardar(deber)
            }
        }
    }

    private var barraSuperior: some View {
        HStack(spacing: 12) {
            CheckboxView(marcado: deber.completado) {
                service.marcar(deber, completado: !deber.completado)
            }
            Divider().frame(height: 16)

            Button {
                mostrandoFecha.toggle()
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "calendar")
                    Text(deber.fechaEntrega.map { FechaFormato.completo($0) } ?? "Fecha de vencimiento")
                }
                .foregroundColor(deber.vencido ? .red : (deber.fechaEntrega == nil ? .secondary : .accentColor))
            }
            .buttonStyle(.plain)
            .popover(isPresented: $mostrandoFecha, arrowEdge: .bottom) {
                FechaPopover(deber: deber, service: service, visible: $mostrandoFecha)
            }

            Spacer()

            Button {
                model.elegirArchivos()
            } label: {
                Image(systemName: "paperclip")
            }
            .buttonStyle(.plain)
            .help("Adjuntar PDF o Word")

            Menu {
                Button("Eliminar tarea", role: .destructive, action: onDelete)
            } label: {
                Image(systemName: "ellipsis")
            }
            .menuStyle(.borderlessButton)
            .fixedSize()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
    }

    private var pie: some View {
        HStack {
            Menu {
                ForEach(materias) { materia in
                    Button {
                        service.mover(deber, a: materia)
                    } label: {
                        Label(materia.nombre, systemImage: materia.icono)
                    }
                }
            } label: {
                Label(deber.materia.nombre, systemImage: deber.materia.icono)
            }
            .menuStyle(.borderlessButton)
            .fixedSize()

            Spacer()
            Text("Editado \(deber.fechaActualizacion.formatted(date: .abbreviated, time: .shortened))")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
    }
}

struct FechaPopover: View {
    @ObservedObject var deber: Deber
    let service: DeberService
    @Binding var visible: Bool

    private var cal: Calendar { .current }

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                atajo("Hoy", dias: 0)
                atajo("Mañana", dias: 1)
                atajo("+7 días", dias: 7)
            }

            DatePicker("", selection: Binding(
                get: { deber.fechaEntrega ?? .now },
                set: { fijarDia($0) }
            ), displayedComponents: .date)
            .datePickerStyle(.graphical)
            .labelsHidden()

            if let fecha = deber.fechaEntrega {
                HStack {
                    Text("Hora")
                    Spacer()
                    DatePicker("", selection: Binding(
                        get: { fecha },
                        set: { deber.fechaEntrega = $0; service.guardar(deber) }
                    ), displayedComponents: .hourAndMinute)
                    .labelsHidden()
                }

                HStack {
                    Text("Recordatorio")
                    Spacer()
                    Picker("", selection: Binding(
                        get: { Int(deber.recordatorioAntesEnHoras) },
                        set: { deber.recordatorioAntesEnHoras = Int16($0); service.guardar(deber) }
                    )) {
                        Text("Sin recordatorio").tag(0)
                        Text("24 horas antes").tag(24)
                        Text("48 horas antes").tag(48)
                    }
                    .labelsHidden()
                    .fixedSize()
                }

                Button("Quitar fecha", role: .destructive) {
                    deber.fechaEntrega = nil
                    service.guardar(deber)
                    visible = false
                }
            }
        }
        .padding(16)
        .frame(width: 290)
    }

    private func atajo(_ titulo: String, dias: Int) -> some View {
        Button(titulo) {
            fijarDia(cal.date(byAdding: .day, value: dias, to: .now) ?? .now)
        }
    }

    private func fijarDia(_ dia: Date) {
        let hora = deber.fechaEntrega.map { cal.dateComponents([.hour, .minute], from: $0) }
            ?? DateComponents(hour: 18, minute: 0)
        deber.fechaEntrega = cal.date(bySettingHour: hora.hour ?? 18, minute: hora.minute ?? 0, second: 0, of: dia)
        service.guardar(deber)
    }
}

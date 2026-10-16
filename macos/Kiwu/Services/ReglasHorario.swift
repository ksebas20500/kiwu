import Foundation

/// Datos de una clase para las reglas del horario: sin Core Data, para poder probarlas (réplica de `Clase` en Android).
/// Las horas son «flotantes»: minutos desde las 00:00 en hora local, sin zona horaria.
struct ClaseHorario: Equatable, Identifiable {
    var id = UUID()
    var horario = UUID()
    var materiaId: UUID?
    var grupo = UUID()
    var titulo: String
    /// 1 = lunes … 7 = domingo.
    var diaSemana: Int
    var inicioMin: Int
    var finMin: Int
    var aula = ""
    var notas = ""
    /// Minutos de antelación del aviso; nil = sin aviso.
    var avisoMin: Int?
}

/// Reglas del horario: funciones puras (réplica de `ReglasHorario` de Android y Linux).
enum ReglasHorario {
    static let minutosDia = 1440

    static let diasLargos = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"]
    static let diasCortos = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"]
    static let diasLetra = ["L", "M", "X", "J", "V", "S", "D"]
    /// Opciones de aviso, en minutos de antelación.
    static let avisos = [5, 10, 15, 30, 60]

    static func nombreDia(_ dia: Int) -> String { diasLargos[min(max(dia - 1, 0), 6)] }

    /// «HH:mm»; 1440 se escribe «24:00».
    static func hora(_ minutos: Int) -> String { String(format: "%02d:%02d", minutos / 60, minutos % 60) }

    static func rango(_ clase: ClaseHorario) -> String { "\(hora(clase.inicioMin)) – \(hora(clase.finMin))" }

    /// Día ISO (1 = lunes … 7 = domingo) de una fecha.
    static func diaDe(_ fecha: Date, calendario: Calendar = .current) -> Int {
        (calendario.component(.weekday, from: fecha) + 5) % 7 + 1
    }

    /// Minutos desde las 00:00 de una fecha en la hora local del calendario.
    static func minutoDe(_ fecha: Date, calendario: Calendar = .current) -> Int {
        calendario.component(.hour, from: fecha) * 60 + calendario.component(.minute, from: fecha)
    }

    /// Minutos desde las 00:00 de una hora elegida en un selector. En el fin, las 00:00 son la medianoche del final (1440).
    static func minutos(de fecha: Date, esFin: Bool, calendario: Calendar = .current) -> Int {
        let m = minutoDe(fecha, calendario: calendario)
        return esFin && m == 0 ? minutosDia : m
    }

    /// Una hora de hoy para mostrarla en un selector (1440 se muestra como las 00:00).
    static func fecha(deMinutos minutos: Int, calendario: Calendar = .current) -> Date {
        let m = minutos % minutosDia
        return calendario.date(bySettingHour: m / 60, minute: m % 60, second: 0, of: .now) ?? .now
    }

    /// Una clase necesita que el fin sea posterior al inicio, dentro del mismo día.
    static func valida(inicio: Int, fin: Int) -> Bool {
        (0..<minutosDia).contains(inicio) && (1...minutosDia).contains(fin) && fin > inicio
    }

    static func deDia(_ clases: [ClaseHorario], _ dia: Int) -> [ClaseHorario] {
        clases.filter { $0.diaSemana == dia }.sorted {
            if $0.inicioMin != $1.inicioMin { return $0.inicioMin < $1.inicioMin }
            if $0.finMin != $1.finMin { return $0.finMin < $1.finMin }
            return $0.titulo < $1.titulo
        }
    }

    /// Terminar a las 10:00 y empezar a las 10:00 no chocan.
    static func seSolapan(_ a: ClaseHorario, _ b: ClaseHorario) -> Bool {
        a.diaSemana == b.diaSemana && a.inicioMin < b.finMin && b.inicioMin < a.finMin
    }

    /// Clases ya existentes (de **otro** grupo) con las que chocaría una clase nueva o editada.
    /// Es una advertencia: no impide guardar.
    static func choques(existentes: [ClaseHorario], grupo: UUID?, dias: Set<Int>, inicio: Int, fin: Int) -> [ClaseHorario] {
        existentes
            .filter { $0.grupo != grupo && dias.contains($0.diaSemana) && $0.inicioMin < fin && inicio < $0.finMin }
            .sorted { ($0.diaSemana, $0.inicioMin) < ($1.diaSemana, $1.inicioMin) }
    }

    /// La clase en curso en ese día y minuto (el minuto de fin ya no cuenta).
    static func enCurso(_ clases: [ClaseHorario], dia: Int, minuto: Int) -> ClaseHorario? {
        deDia(clases, dia).first { minuto >= $0.inicioMin && minuto < $0.finMin }
    }

    /// La siguiente clase que empieza después de ese momento (hoy o en los próximos 7 días) y cuántos días faltan.
    static func siguiente(_ clases: [ClaseHorario], dia: Int, minuto: Int) -> (clase: ClaseHorario, dias: Int)? {
        for desplazamiento in 0...7 {
            let d = (dia - 1 + desplazamiento) % 7 + 1
            if let candidata = deDia(clases, d).first(where: { desplazamiento > 0 || $0.inicioMin > minuto }) {
                return (candidata, desplazamiento)
            }
        }
        return nil
    }

    /// Cuándo empieza la clase la próxima vez que toca, a partir de `desde` (inclusive).
    static func proximoInicio(_ clase: ClaseHorario, desde: Date, calendario: Calendar = .current) -> Date {
        let dia = min(max(clase.diaSemana, 1), 7)
        var inicioDelDia = calendario.startOfDay(for: desde)
        for _ in 0..<7 where diaDe(inicioDelDia, calendario: calendario) != dia {
            inicioDelDia = calendario.date(byAdding: .day, value: 1, to: inicioDelDia) ?? inicioDelDia
        }
        var inicio = calendario.date(byAdding: .minute, value: clase.inicioMin, to: inicioDelDia) ?? inicioDelDia
        if inicio < desde {
            let semana = calendario.date(byAdding: .day, value: 7, to: inicioDelDia) ?? inicioDelDia
            inicio = calendario.date(byAdding: .minute, value: clase.inicioMin, to: semana) ?? semana
        }
        return inicio
    }

    /// Momento en que debe sonar el aviso de la próxima clase, o nil si no tiene aviso.
    /// Si el aviso de esta semana ya pasó, se pasa a la semana siguiente (hasta 3 intentos).
    static func momentoDeAviso(_ clase: ClaseHorario, desde: Date, calendario: Calendar = .current) -> Date? {
        guard let antelacion = clase.avisoMin else { return nil }
        var cursor = desde
        for _ in 0..<3 {
            let inicio = proximoInicio(clase, desde: cursor, calendario: calendario)
            let aviso = inicio.addingTimeInterval(-Double(antelacion) * 60)
            if aviso >= desde { return aviso }
            cursor = inicio.addingTimeInterval(60)
        }
        return nil
    }

    /// «Empieza a las 08:00» o «Empieza a las 08:00 · Aula 204».
    static func textoAviso(_ clase: ClaseHorario) -> String {
        let aula = clase.aula.trimmingCharacters(in: .whitespacesAndNewlines)
        return ["Empieza a las \(hora(clase.inicioMin))", aula.isEmpty ? nil : aula].compactMap { $0 }.joined(separator: " · ")
    }

    /// Cuántos días faltan hasta la siguiente clase, dicho en palabras.
    static func cuando(dias: Int, dia: Int) -> String {
        switch dias {
        case 0: return "hoy"
        case 1: return "mañana"
        default: return nombreDia(dia).lowercased()
        }
    }
}

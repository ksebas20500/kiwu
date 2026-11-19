import { useEffect, useRef, useState } from "react";

const ArrowRight = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M5 12h14M13 6l6 6-6 6" />
  </svg>
);

const Check = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="m5 12 4 4L19 6" />
  </svg>
);

const CalendarIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M6 3v3m12-3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z" />
  </svg>
);

const ChartIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M5 19V9m7 10V5m7 14v-7" />
  </svg>
);

const ClockIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="12" cy="12" r="8" />
    <path d="M12 7v5l3 2" />
  </svg>
);

function Mascot({ small = false }: { small?: boolean }) {
  return (
    <div className={`mascot-wrap ${small ? "mascot-small" : ""}`}>
      <div className="mascot-glow" />
      <img src="/assets/kiwu-mascot.png" alt="Kiwu, el gato mago" className="mascot" />
      <span className="spark spark-one">✦</span>
      <span className="spark spark-two">✦</span>
      <span className="spark spark-three">·</span>
    </div>
  );
}

function TasksScene() {
  return (
    <div className="scene tasks-scene">
      <div className="orb orb-peach" />
      <Mascot />
      <div className="task-card task-card-one">
        <span className="task-check checked">
          <Check />
        </span>
        <div>
          <strong>Entregar ensayo</strong>
          <small>Literatura</small>
        </div>
      </div>
      <div className="task-card task-card-two">
        <span className="task-check" />
        <div>
          <strong>Quiz de cálculo</strong>
          <small>Viernes · 9:00</small>
        </div>
      </div>
      <div className="due-chip">
        <span>●</span> Vence mañana · Modelado
      </div>
      <div className="confetti">
        <i />
        <i />
        <i />
        <i />
        <i />
      </div>
    </div>
  );
}

function ScheduleScene() {
  const days = ["L", "M", "X", "J", "V", "S", "D"];
  return (
    <div className="scene schedule-scene">
      <div className="scene-label">
        <ClockIcon /> PRÓXIMA CLASE
      </div>
      <div className="days-row">
        {days.map((day) => (
          <span className={day === "X" ? "active" : ""} key={day}>
            {day}
          </span>
        ))}
      </div>
      <div className="class-card">
        <div className="class-accent" />
        <div className="class-top">
          <div>
            <small>08:00 — 10:00</small>
            <strong>Modelado 3D</strong>
          </div>
          <span className="room">B-204</span>
        </div>
        <div className="class-bottom">
          <div className="avatars">
            <i>K</i>
            <i>M</i>
            <i>+8</i>
          </div>
          <div className="absence">
            <span className="absence-zero">0/4</span>
            <span className="absence-one">1/4</span> faltas
          </div>
        </div>
      </div>
      <div className="warning-pulse">
        <span>!</span> Falta registrada
      </div>
      <div className="corner-mascot">
        <img src="/assets/kiwu-mascot.png" alt="" />
      </div>
    </div>
  );
}

function GradesScene() {
  return (
    <div className="scene grades-scene">
      <div className="grade-header">
        <span>QUÍMICA</span>
        <span className="grade-average">
          4.2 <small>promedio</small>
        </span>
      </div>
      <div className="progress-copy">
        <strong>Progreso del curso</strong>
        <span>35% evaluado</span>
      </div>
      <div className="progress-track">
        <div className="progress-value" />
      </div>
      <div className="grade-pills">
        <div>
          <span>Parcial 1</span>
          <strong>4.90</strong>
          <small>20%</small>
        </div>
        <div>
          <span>Talleres</span>
          <strong>3.50</strong>
          <small>15%</small>
        </div>
      </div>
      <div className="need-card">
        <span className="need-icon">
          <ChartIcon />
        </span>
        <div>
          <small>PARA APROBAR CON 3.0</small>
          <strong>
            Necesitas <b>2.14</b> en el resto
          </strong>
        </div>
      </div>
      <div className="grade-stars">
        ✦ <span>✦</span>
      </div>
    </div>
  );
}

function CalendarScene() {
  const dates = Array.from({ length: 28 }, (_, i) => i + 1);
  return (
    <div className="scene calendar-scene">
      <div className="calendar-top">
        <div>
          <small>NOVIEMBRE</small>
          <strong>Tu mes, de un vistazo</strong>
        </div>
        <span>2026</span>
      </div>
      <div className="week-labels">
        {["L", "M", "X", "J", "V", "S", "D"].map((d) => (
          <span key={d}>{d}</span>
        ))}
      </div>
      <div className="calendar-grid">
        {dates.map((d) => (
          <span
            key={d}
            className={`${d === 12 ? "today" : ""} ${[4, 9, 17, 23].includes(d) ? "has-event" : ""}`}
          >
            {d}
          </span>
        ))}
      </div>
      <div className="sync-lines">
        <i />
        <i />
        <i />
      </div>
      <div className="mini-nav">
        <div>
          <span>
            <Check />
          </span>
          <small>Tareas</small>
        </div>
        <div className="active">
          <span>
            <ChartIcon />
          </span>
          <small>Notas</small>
        </div>
        <div>
          <span>
            <ClockIcon />
          </span>
          <small>Horario</small>
        </div>
      </div>
      <div className="celebrate-mascot">
        <img src="/assets/kiwu-mascot.png" alt="" />
        <span>✦</span>
      </div>
    </div>
  );
}

const slides = [
  {
    eyebrow: "TU SEMESTRE, MÁS SIMPLE",
    title: "Organiza tu semestre sin estrés",
    subtitle:
      "Crea listas por materia, gestiona entregas con fechas límite inteligentes y mantén tu racha al día.",
    scene: <TasksScene />,
  },
  {
    eyebrow: "SIEMPRE A TIEMPO",
    title: "Tu horario y asistencia bajo control",
    subtitle:
      "Visualiza tus clases de la semana y lleva la cuenta exacta de tus faltas para cuidar cada materia.",
    scene: <ScheduleScene />,
  },
  {
    eyebrow: "SIN MÁS CÁLCULOS MENTALES",
    title: "Calcula tu nota antes del examen final",
    subtitle:
      "Registra tus notas y porcentajes. Kiwu te dice exactamente cuánto necesitas para aprobar.",
    scene: <GradesScene />,
  },
  {
    eyebrow: "TODO CONECTADO",
    title: "Todo tu mundo académico en un solo lugar",
    subtitle: "Accede a tus tareas, notas y horarios en cualquier momento. ¡Listo para empezar!",
    scene: <CalendarScene />,
  },
];

export default function App() {
  const [current, setCurrent] = useState(0);
  const touchStart = useRef(0);

  const goTo = (index: number) => setCurrent(Math.max(0, Math.min(slides.length - 1, index)));

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "ArrowRight") goTo(current + 1);
      if (event.key === "ArrowLeft") goTo(current - 1);
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [current]);

  return (
    <main className="app-shell">
      <div
        className="phone"
        onTouchStart={(event) => {
          touchStart.current = event.touches[0].clientX;
        }}
        onTouchEnd={(event) => {
          const distance = touchStart.current - event.changedTouches[0].clientX;
          if (Math.abs(distance) > 45) goTo(current + (distance > 0 ? 1 : -1));
        }}
      >
        <div className="ambient ambient-one" />
        <div className="ambient ambient-two" />
        <header className="topbar">
          <button className="brand" onClick={() => goTo(0)} aria-label="Volver al inicio">
            <img src="/assets/kiwu-icon.png" alt="" />
            <span>kiwu</span>
          </button>
          {current < 3 && (
            <button className="skip" onClick={() => goTo(3)}>
              Saltar
            </button>
          )}
        </header>

        <section key={current} className={`slide slide-${current + 1}`}>
          {slides[current].scene}
          <div className="copy">
            <p className="eyebrow">{slides[current].eyebrow}</p>
            <h1>{slides[current].title}</h1>
            <p className="subtitle">{slides[current].subtitle}</p>
          </div>
        </section>

        <footer className="controls">
          <div className="dots" aria-label={`Paso ${current + 1} de 4`}>
            {slides.map((_, index) => (
              <button
                key={index}
                className={index === current ? "active" : ""}
                onClick={() => goTo(index)}
                aria-label={`Ir al paso ${index + 1}`}
              />
            ))}
          </div>
          {current < 3 ? (
            <button
              className="next-button"
              onClick={() => goTo(current + 1)}
              aria-label="Siguiente"
            >
              <ArrowRight />
            </button>
          ) : (
            <button className="start-button" onClick={() => goTo(0)}>
              ¡Empezar ahora! <ArrowRight />
            </button>
          )}
        </footer>
      </div>
    </main>
  );
}

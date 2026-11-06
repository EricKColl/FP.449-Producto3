/*
 * Comportamiento común de todas las páginas. Todo el JavaScript vive en ficheros como este: la política de
 * seguridad de contenidos (SecurityConfig) no ejecuta scripts escritos dentro del HTML.
 * Las animaciones usan GSAP y se desactivan si el sistema pide reducir el movimiento.
 */
(() => {
    'use strict';

    const reducido = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const gsap = window.gsap;
    document.documentElement.classList.add('js');

    // El enlace «Saltar al contenido» lleva al <main> de cada página
    const principal = document.querySelector('main');
    if (principal && !principal.id) principal.id = 'contenido';

    // --- Avisos flotantes: se cierran solos a los 6 segundos o con la X ---
    document.querySelectorAll('.aviso').forEach((aviso, i) => {
        const cerrar = () => {
            aviso.classList.add('saliendo');
            setTimeout(() => aviso.remove(), 350);
        };
        aviso.querySelector('.aviso-cerrar')?.addEventListener('click', cerrar);
        if (aviso.classList.contains('aviso-exito')) setTimeout(cerrar, 6000 + i * 400);
    });

    // --- Confirmación antes de borrar: <form data-confirmar="¿Eliminar «...»?"> ---
    document.addEventListener('submit', (e) => {
        const mensaje = e.target.dataset?.confirmar;
        if (mensaje && !window.confirm(mensaje)) e.preventDefault();
    });

    // --- Consejos emergentes: se abren cada vez que se entra en la página; al cerrarlos queda una burbuja ---
    document.querySelectorAll('.consejo-emergente').forEach((consejo) => {
        const burbuja = document.querySelector(`[data-consejo-abrir="${consejo.dataset.consejo}"]`);
        const abrir = () => {
            consejo.classList.remove('saliendo');
            consejo.classList.add('visible');
            if (burbuja) burbuja.hidden = true;
        };
        const cerrar = () => {
            if (!consejo.classList.contains('visible')) return;
            consejo.classList.add('saliendo');
            setTimeout(() => {
                consejo.classList.remove('visible', 'saliendo');
                if (burbuja) {
                    burbuja.hidden = false;
                    burbuja.focus({ preventScroll: true });
                }
            }, reducido ? 0 : 420);
        };
        consejo.querySelector('.consejo-entendido')?.addEventListener('click', cerrar);
        consejo.querySelector('.consejo-cerrar')?.addEventListener('click', cerrar);
        consejo.addEventListener('keydown', (e) => { if (e.key === 'Escape') cerrar(); });
        burbuja?.addEventListener('click', () => {
            abrir();
            consejo.querySelector('.consejo-entendido')?.focus({ preventScroll: true });
        });
        setTimeout(abrir, reducido ? 0 : 600);
    });

    // --- Ver u ocultar la contraseña: <button data-ver-clave="id-del-campo"> ---
    document.querySelectorAll('[data-ver-clave]').forEach((boton) => {
        const campo = document.getElementById(boton.dataset.verClave);
        if (!campo) return;
        boton.addEventListener('click', () => {
            const ver = campo.type === 'password';
            campo.type = ver ? 'text' : 'password';
            boton.setAttribute('aria-label', ver ? 'Ocultar la contraseña' : 'Mostrar la contraseña');
            boton.querySelector('i').className = ver ? 'ti ti-eye-off' : 'ti ti-eye';
            campo.focus();
        });
    });

    // --- Aviso de mayúsculas activadas al escribir una contraseña ---
    document.querySelectorAll('[data-aviso-mayusculas]').forEach((campo) => {
        const aviso = campo.closest('form')?.querySelector('.aviso-mayusculas');
        if (!aviso) return;
        const comprobar = (e) => { aviso.hidden = !e.getModifierState?.('CapsLock'); };
        campo.addEventListener('keydown', comprobar);
        campo.addEventListener('keyup', comprobar);
        campo.addEventListener('blur', () => { aviso.hidden = true; });
    });

    // --- Formularios <form data-enviando>: el botón muestra que se está enviando y no admite un segundo clic ---
    document.querySelectorAll('form[data-enviando]').forEach((formulario) => {
        formulario.addEventListener('submit', () => {
            const boton = formulario.querySelector('button[type=submit]');
            if (!boton) return;
            boton.classList.add('enviando');
            const icono = boton.querySelector('i');
            if (icono) icono.className = 'ti ti-loader-2';
        });
    });
    // Si se vuelve atrás con el botón del navegador, el botón queda como al principio
    window.addEventListener('pageshow', (e) => {
        if (e.persisted) document.querySelectorAll('.btn.enviando').forEach((b) => b.classList.remove('enviando'));
    });

    // --- Título letra a letra: el texto se mantiene completo para lectores de pantalla ---
    document.querySelectorAll('[data-letras]').forEach((titulo) => {
        const texto = titulo.textContent.trim();
        titulo.setAttribute('aria-label', texto);
        titulo.textContent = '';
        [...texto].forEach((letra) => {
            const span = document.createElement('span');
            span.className = 'letra';
            span.setAttribute('aria-hidden', 'true');
            span.textContent = letra;
            titulo.appendChild(span);
        });
    });

    // --- Inclinación 3D del bloque de título siguiendo al ratón ---
    if (!reducido) {
        document.querySelectorAll('[data-inclinar]').forEach((bloque) => {
            window.addEventListener('pointermove', (e) => {
                const x = (e.clientX / window.innerWidth - 0.5) * 2;
                const y = (e.clientY / window.innerHeight - 0.5) * 2;
                bloque.style.setProperty('--giro-x', `${(-y * 5).toFixed(2)}deg`);
                bloque.style.setProperty('--giro-y', `${(x * 7).toFixed(2)}deg`);
            }, { passive: true });
        });
    }

    // --- Cifras que cuentan desde cero al aparecer ---
    const contar = (elemento) => {
        const final = parseInt(elemento.textContent.replace(/\D/g, ''), 10);
        if (Number.isNaN(final) || reducido || !gsap) return;
        const sufijo = elemento.textContent.replace(/[\d.\s]/g, '');
        const estado = { valor: 0 };
        gsap.to(estado, {
            valor: final, duration: 1.6, ease: 'power3.out',
            onUpdate: () => { elemento.textContent = Math.round(estado.valor) + sufijo; }
        });
    };

    if (!gsap || reducido) return;
    if (window.ScrollTrigger) gsap.registerPlugin(window.ScrollTrigger);
    // Las animaciones siguen el reloj real aunque algún fotograma tarde (equipos lentos o sin tarjeta gráfica)
    gsap.ticker.lagSmoothing(0);

    // Entrada suave del contenido de cada página
    // Solo opacidad: un transform en <main> movería los avisos flotantes, que son position: fixed
    gsap.from('body > main', { opacity: 0, duration: 0.45, ease: 'power2.out', clearProps: 'opacity' });

    // Solo se anima lo que existe en la página: así la consola no se llena de avisos
    const hay = (selector) => document.querySelector(selector) !== null;

    // Entrada del título de la portada
    if (hay('.titulo-3d .letra')) {
        gsap.from('.titulo-3d .letra', {
            yPercent: 110, rotateX: -80, opacity: 0, duration: 1.1, ease: 'expo.out', stagger: 0.06, delay: 0.25
        });
        gsap.from('.titulo-grupo, .escena-lema, .buscador-portada, .escena-acciones', {
            y: 24, opacity: 0, duration: 0.9, ease: 'power3.out', stagger: 0.12, delay: 0.7
        });
    }

    // Bloques que aparecen al llegar a ellos
    document.querySelectorAll('[data-aparecer]').forEach((bloque) => {
        gsap.from(bloque, {
            y: 34, opacity: 0, duration: 0.85, ease: 'power3.out',
            scrollTrigger: window.ScrollTrigger ? { trigger: bloque, start: 'top 88%', once: true } : undefined
        });
    });
    document.querySelectorAll('.estante, .rejilla-libros').forEach((estante) => {
        gsap.from(estante.querySelectorAll('.cubierta'), {
            y: 40, opacity: 0, rotateY: -18, duration: 0.8, ease: 'power3.out', stagger: 0.07,
            scrollTrigger: window.ScrollTrigger ? { trigger: estante, start: 'top 85%', once: true } : undefined
        });
    });

    document.querySelectorAll('[data-contar]').forEach((cifra) => {
        if (window.ScrollTrigger) {
            window.ScrollTrigger.create({ trigger: cifra, start: 'top 92%', once: true, onEnter: () => contar(cifra) });
        } else {
            contar(cifra);
        }
    });
})();

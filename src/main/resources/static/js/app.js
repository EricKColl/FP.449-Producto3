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
    // Se abre una ventana propia (<dialog>) en lugar de window.confirm. Escucha en fase de captura para frenar el
    // envío antes que cualquier otro manejador del formulario (por ejemplo, data-enviando). Al aceptar, el
    // formulario se vuelve a enviar marcado como confirmado. data-confirmar-boton cambia el texto del botón.
    let ventanaConfirmar = null;
    const confirmar = (formulario) => {
        if (!ventanaConfirmar) {
            ventanaConfirmar = document.createElement('dialog');
            ventanaConfirmar.className = 'ventana-confirmar';
            ventanaConfirmar.setAttribute('aria-labelledby', 'ventana-confirmar-texto');
            ventanaConfirmar.innerHTML =
                '<form method="dialog" class="ventana-confirmar-cuerpo">' +
                '<span class="ventana-confirmar-icono" aria-hidden="true"><i class="ti ti-alert-triangle"></i></span>' +
                '<p class="ventana-confirmar-texto" id="ventana-confirmar-texto"></p>' +
                '<p class="ventana-confirmar-nota">Esta acción no se puede deshacer.</p>' +
                '<div class="ventana-confirmar-botones">' +
                '<button type="submit" value="no" class="btn btn-sesion">No, volver</button>' +
                '<button type="submit" value="si" class="btn btn-peligro"><i class="ti ti-trash"></i> <span></span></button>' +
                '</div></form>';
            document.body.appendChild(ventanaConfirmar);
            ventanaConfirmar.addEventListener('click', (e) => {
                if (e.target === ventanaConfirmar) ventanaConfirmar.close('no'); // clic fuera de la ventana
            });
        }
        ventanaConfirmar.querySelector('.ventana-confirmar-texto').textContent = formulario.dataset.confirmar;
        ventanaConfirmar.querySelector('.btn-peligro span').textContent = formulario.dataset.confirmarBoton || 'Sí, eliminar';
        ventanaConfirmar.returnValue = 'no';
        ventanaConfirmar.onclose = () => {
            if (ventanaConfirmar.returnValue !== 'si') return;
            formulario.dataset.confirmado = 'si';
            formulario.requestSubmit();
        };
        ventanaConfirmar.showModal();
        ventanaConfirmar.querySelector('.btn-sesion').focus();
    };
    document.addEventListener('submit', (e) => {
        const formulario = e.target;
        if (!formulario.dataset?.confirmar) return;
        if (formulario.dataset.confirmado === 'si') {
            delete formulario.dataset.confirmado;
            return;
        }
        e.preventDefault();
        e.stopPropagation();
        if (typeof HTMLDialogElement === 'function') confirmar(formulario);
        else if (window.confirm(formulario.dataset.confirmar)) {
            formulario.dataset.confirmado = 'si';
            formulario.requestSubmit();
        }
    }, true);

    // --- Vista previa de los formularios de libro, autor y usuario: <form data-vista-previa> con campos
    // [data-previa="clave"] que copian lo escrito en los [data-previa-ver="clave"] del mismo panel ---
    document.querySelectorAll('form[data-vista-previa]').forEach((formulario) => {
        const panel = formulario.closest('.formulario-panel');
        if (!panel) return;
        const tipo = formulario.dataset.vistaPrevia;
        const ver = (clave, texto, vacio) => {
            panel.querySelectorAll(`[data-previa-ver="${clave}"]`).forEach((el) => {
                el.textContent = texto.trim() || vacio;
            });
        };
        const valor = (clave) => formulario.querySelector(`[data-previa="${clave}"]`)?.value ?? '';
        const actualizar = () => {
            if (tipo === 'libro') {
                ver('titulo', valor('titulo'), 'Título del libro');
                ver('genero', valor('genero'), 'Género');
                const autores = [...formulario.querySelectorAll('[data-previa="autor"]:checked')].map((c) => c.dataset.nombre);
                ver('autor', autores.join(', '), 'Autor');
                return;
            }
            const nombre = valor('nombre').trim();
            const apellidos = valor('apellidos').trim();
            ver('nombreCompleto', `${nombre} ${apellidos}`, tipo === 'autor' ? 'Nombre del autor' : 'Nombre y apellidos');
            ver('iniciales', (nombre.charAt(0) + apellidos.charAt(0)).toUpperCase(), '—');
            if (tipo === 'autor') ver('nacionalidad', valor('nacionalidad'), 'Nacionalidad');
            if (tipo === 'usuario') {
                ver('email', valor('email'), 'email@ejemplo.com');
                const rol = formulario.querySelector('[data-previa="rol"]');
                const insignia = panel.querySelector('[data-previa-ver="rol"]');
                if (rol && insignia) {
                    insignia.textContent = rol.options[rol.selectedIndex]?.text ?? '';
                    insignia.classList.toggle('rol-admin', rol.value === 'ADMIN');
                }
            }
        };
        formulario.addEventListener('input', actualizar);
        formulario.addEventListener('change', actualizar);
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

    // --- Catálogos filtrables: <section data-catalogo> con elementos [data-item] en una o varias [data-lista].
    // Filtra por texto (data-texto), por género (data-genero) y por disponibilidad (data-disponible), ordena por
    // data-titulo, data-autor o data-anio y alterna entre vistas (data-vista). Los datos los pone Thymeleaf.
    document.querySelectorAll('[data-catalogo]').forEach((catalogo) => {
        const texto = catalogo.querySelector('[data-filtro-texto]');
        const disponible = catalogo.querySelector('[data-filtro-disponible]');
        const orden = catalogo.querySelector('[data-orden]');
        const contador = catalogo.querySelector('[data-contador]');
        const vacio = catalogo.querySelector('[data-vacio]');
        const listas = [...catalogo.querySelectorAll('[data-lista]')];
        const generos = [...catalogo.querySelectorAll('[data-filtro-genero]')];
        let genero = '';
        // Sin tildes: «garcia» encuentra a «García»
        const normalizar = (v) => v.toLowerCase().normalize('NFD').replace(/\p{Diacritic}/gu, '').trim();

        const aplicar = () => {
            const buscado = normalizar(texto?.value ?? '');
            let visibles = 0;
            listas.forEach((lista, indice) => {
                lista.querySelectorAll('[data-item]').forEach((item) => {
                    const ok = (!buscado || normalizar(item.dataset.texto ?? '').includes(buscado))
                        && (!genero || item.dataset.genero === genero)
                        && (!disponible?.checked || item.dataset.disponible === 'true');
                    item.hidden = !ok;
                    if (ok && indice === 0) visibles++;
                });
            });
            if (contador) contador.textContent = visibles;
            if (vacio) vacio.classList.toggle('d-none', visibles > 0);
        };

        const ordenar = () => {
            if (!orden) return;
            const [campo, sentido] = orden.value.split('-');
            listas.forEach((lista) => {
                const items = [...lista.querySelectorAll(':scope > [data-item]')];
                items.sort((a, b) => {
                    const va = a.dataset[campo] ?? '';
                    const vb = b.dataset[campo] ?? '';
                    const numerico = campo === 'anio' || campo === 'libros';
                    const r = numerico ? Number(va) - Number(vb) : va.localeCompare(vb, 'es');
                    return sentido === 'desc' ? -r : r;
                });
                items.forEach((item) => lista.appendChild(item));
            });
        };

        // Vista de estante o de tabla, recordada en el navegador
        const vistas = [...catalogo.querySelectorAll('[data-vista]')];
        const ponerVista = (vista) => {
            catalogo.dataset.vistaActual = vista;
            vistas.forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.vista === vista)));
            try { localStorage.setItem('vista-catalogo', vista); } catch { /* sin almacenamiento */ }
        };
        if (vistas.length) {
            let guardada = null;
            try { guardada = localStorage.getItem('vista-catalogo'); } catch { /* sin almacenamiento */ }
            ponerVista(guardada || catalogo.dataset.vistaInicial || 'estante');
            vistas.forEach((b) => b.addEventListener('click', () => ponerVista(b.dataset.vista)));
        }

        texto?.addEventListener('input', aplicar);
        disponible?.addEventListener('change', aplicar);
        orden?.addEventListener('change', () => { ordenar(); aplicar(); });
        generos.forEach((boton) => boton.addEventListener('click', () => {
            genero = boton.dataset.filtroGenero;
            generos.forEach((g) => g.classList.toggle('activo', g === boton));
            aplicar();
        }));
    });

    // --- Nuevo préstamo: el resumen muestra el usuario y el libro elegidos ---
    document.querySelectorAll('form[data-prestamo]').forEach((formulario) => {
        formulario.addEventListener('change', (e) => {
            const opcion = e.target;
            if (opcion.type !== 'radio') return;
            const destino = formulario.querySelector(`[data-resumen="${opcion.name}"]`);
            if (destino) {
                destino.textContent = opcion.dataset.nombre;
                destino.classList.add('elegido');
            }
        });
    });

    // --- Portada grande de la ficha: se inclina en 3D siguiendo al ratón ---
    if (!reducido) {
        document.querySelectorAll('[data-inclinar-portada]').forEach((zona) => {
            const portada = zona.firstElementChild;
            zona.addEventListener('pointermove', (e) => {
                const r = zona.getBoundingClientRect();
                const x = (e.clientX - r.left) / r.width - 0.5;
                const y = (e.clientY - r.top) / r.height - 0.5;
                portada.style.transform = `rotateY(${(x * 22).toFixed(1)}deg) rotateX(${(-y * 16).toFixed(1)}deg)`;
                portada.style.setProperty('--brillo-x', `${((x + 0.5) * 100).toFixed(0)}%`);
            });
            zona.addEventListener('pointerleave', () => { portada.style.transform = ''; });
        });
    }

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

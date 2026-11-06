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

    // --- Portada: al volver a ella todo aparece al instante ---
    // La corona enseña la última imagen que dibujó el 3D (la guarda js/corona3d.js al salir) mientras el 3D se
    // prepara, y la entrada animada del título solo se ve la primera vez de cada sesión
    const escenaCorona = document.querySelector('.escena-corona');
    let portadaYaVista = false;
    if (escenaCorona) {
        try {
            portadaYaVista = sessionStorage.getItem('backlord-portada') === 'vista';
            sessionStorage.setItem('backlord-portada', 'vista');
            const foto = sessionStorage.getItem('backlord-corona-foto');
            if (foto) {
                const imagen = new Image();
                imagen.className = 'corona-instantanea';
                imagen.alt = '';
                imagen.decoding = 'sync';
                imagen.src = foto;
                escenaCorona.querySelector('canvas')?.before(imagen);
                escenaCorona.classList.add('con-foto');
            }
        } catch {
            // Sin sessionStorage se ve la entrada normal
        }
    }

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

    // --- Aviso antes de que la sesión caduque por inactividad: <meta name="sesion-caduca" content="segundos">.
    // Dos minutos antes sale un aviso con «Seguir conectado»; si nadie lo pulsa, al acabar el plazo se ofrece
    // volver a entrar. Las pestañas se avisan entre sí (localStorage), así renovar en una vale para todas ---
    const metaSesion = document.querySelector('meta[name="sesion-caduca"]');
    if (metaSesion) {
        const duracion = parseInt(metaSesion.content, 10) * 1000;
        const margen = Math.min(120000, duracion / 4);
        let ultimaActividad = Date.now();
        let avisoSesion = null;
        let temporizador = 0;
        let cuenta = 0;
        const formato = (ms) => {
            const s = Math.max(0, Math.ceil(ms / 1000));
            return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
        };
        const cerrarAviso = () => {
            clearInterval(cuenta);
            avisoSesion?.remove();
            avisoSesion = null;
        };
        const programar = () => {
            clearTimeout(temporizador);
            temporizador = setTimeout(mostrarAviso, Math.max(0, ultimaActividad + duracion - margen - Date.now()));
        };
        const marcarActividad = (momento) => {
            ultimaActividad = momento;
            cerrarAviso();
            programar();
        };
        const renovar = async () => {
            try {
                const respuesta = await fetch('/sesion/renovar', { credentials: 'same-origin', redirect: 'manual' });
                if (respuesta.status !== 204) throw new Error('caducada');
                const ahora = Date.now();
                try { localStorage.setItem('backlord-sesion', String(ahora)); } catch { /* sin almacenamiento */ }
                marcarActividad(ahora);
            } catch {
                location.href = `/login?caducada&volver=${encodeURIComponent(location.pathname + location.search)}`;
            }
        };
        const mostrarAviso = () => {
            if (avisoSesion) return;
            avisoSesion = document.createElement('div');
            avisoSesion.className = 'aviso-sesion';
            avisoSesion.setAttribute('role', 'alertdialog');
            avisoSesion.setAttribute('aria-labelledby', 'aviso-sesion-texto');
            avisoSesion.innerHTML = '<span class="aviso-sesion-icono" aria-hidden="true"><i class="ti ti-clock-hour-4"></i></span>'
                + '<p id="aviso-sesion-texto"></p><button type="button" class="btn btn-primary btn-sm"></button>';
            const texto = avisoSesion.querySelector('p');
            const boton = avisoSesion.querySelector('button');
            boton.textContent = 'Seguir conectado';
            boton.addEventListener('click', renovar);
            document.body.appendChild(avisoSesion);
            boton.focus({ preventScroll: true });
            const actualizar = () => {
                const queda = ultimaActividad + duracion - Date.now();
                if (queda > 0) {
                    texto.textContent = `Por inactividad, tu sesión se cerrará en ${formato(queda)}`;
                    return;
                }
                clearInterval(cuenta);
                texto.textContent = 'Tu sesión se ha cerrado por inactividad';
                boton.textContent = 'Volver a entrar';
                boton.onclick = () => {
                    location.href = `/login?caducada&volver=${encodeURIComponent(location.pathname + location.search)}`;
                };
                boton.removeEventListener('click', renovar);
            };
            actualizar();
            cuenta = setInterval(actualizar, 1000);
        };
        try { localStorage.setItem('backlord-sesion', String(ultimaActividad)); } catch { /* sin almacenamiento */ }
        window.addEventListener('storage', (e) => {
            if (e.key === 'backlord-sesion' && e.newValue) marcarActividad(parseInt(e.newValue, 10));
        });
        programar();
    }

    // --- Campanita de avisos: el navegador recuerda qué avisos ya se vieron (por su clave) y solo cuenta los
    // nuevos. Al abrir el menú quedan todos vistos. Los avisos que ya no existen se olvidan solos ---
    // En tiempo real: la página escucha /avisos/flujo (Server-Sent Events) y, si llega una lista nueva, repinta
    // el menú, hace sonar la campana y enseña el aviso nuevo en una notificación flotante
    const campana = document.querySelector('[data-campana]');
    if (campana) {
        const menu = campana.querySelector('.menu-avisos');
        const boton = campana.querySelector('.campana-boton');
        const leerVistos = () => {
            try { return JSON.parse(localStorage.getItem('backlord-avisos-vistos') || '[]'); } catch { return []; }
        };
        const actualizarCuenta = () => {
            const items = [...campana.querySelectorAll('[data-aviso]')];
            const vistos = leerVistos();
            const nuevos = items.filter((item) => !vistos.includes(item.dataset.aviso));
            items.forEach((item) => item.classList.toggle('nuevo', nuevos.includes(item)));
            let cuenta = campana.querySelector('.campana-cuenta');
            if (!cuenta && nuevos.length) {
                cuenta = document.createElement('span');
                cuenta.className = 'campana-cuenta';
                cuenta.setAttribute('aria-hidden', 'true');
                boton.appendChild(cuenta);
            }
            if (cuenta) cuenta.textContent = String(nuevos.length);
            campana.classList.toggle('todo-visto', nuevos.length === 0);
            campana.classList.toggle('con-nuevos', nuevos.length > 0);
            boton.setAttribute('aria-label', nuevos.length
                ? `Avisos: ${nuevos.length} sin ver de ${items.length}` : `Avisos: ${items.length || 'ninguno'}`);
        };
        actualizarCuenta();
        boton.addEventListener('shown.bs.dropdown', () => {
            const claves = [...campana.querySelectorAll('[data-aviso]')].map((i) => i.dataset.aviso);
            try { localStorage.setItem('backlord-avisos-vistos', JSON.stringify(claves)); } catch { /* sin almacenamiento */ }
            actualizarCuenta();
        });

        // Repinta el menú con la lista que manda el servidor (los textos van con textContent, nunca como HTML)
        const pintarAvisos = (avisos) => {
            menu.querySelectorAll('.aviso-item, .menu-avisos-vacio').forEach((el) => el.remove());
            avisos.forEach((aviso) => {
                const enlace = document.createElement('a');
                enlace.className = `aviso-item tono-${aviso.tono}`;
                enlace.href = aviso.enlace;
                enlace.dataset.aviso = aviso.clave;
                const icono = document.createElement('span');
                icono.className = 'aviso-item-icono';
                icono.setAttribute('aria-hidden', 'true');
                const simbolo = document.createElement('i');
                simbolo.className = `ti ti-${aviso.icono}`;
                icono.appendChild(simbolo);
                const texto = document.createElement('span');
                texto.className = 'aviso-item-texto';
                const titulo = document.createElement('strong');
                titulo.textContent = aviso.titulo;
                const detalle = document.createElement('small');
                detalle.textContent = aviso.texto;
                texto.append(titulo, detalle);
                enlace.append(icono, texto);
                menu.appendChild(enlace);
            });
            if (!avisos.length) {
                const vacio = document.createElement('p');
                vacio.className = 'menu-avisos-vacio';
                const simbolo = document.createElement('i');
                simbolo.className = 'ti ti-circle-check';
                vacio.append(simbolo, ' Todo al día: no tienes avisos.');
                menu.appendChild(vacio);
            }
        };
        // Notificación flotante con el aviso que acaba de llegar
        const notificar = (aviso) => {
            let zona = document.querySelector('.avisos');
            if (!zona) {
                zona = document.createElement('div');
                zona.className = 'avisos';
                document.body.appendChild(zona);
            }
            const nota = document.createElement('div');
            nota.className = `aviso aviso-vivo ${aviso.tono === 'peligro' ? 'aviso-error' : 'aviso-exito'}`;
            nota.setAttribute('role', 'status');
            const simbolo = document.createElement('i');
            simbolo.className = `ti ti-${aviso.icono}`;
            const enlace = document.createElement('a');
            enlace.href = aviso.enlace;
            enlace.textContent = aviso.titulo;
            const cerrar = document.createElement('button');
            cerrar.type = 'button';
            cerrar.className = 'aviso-cerrar';
            cerrar.setAttribute('aria-label', 'Cerrar el aviso');
            const x = document.createElement('i');
            x.className = 'ti ti-x';
            cerrar.appendChild(x);
            const quitar = () => {
                nota.classList.add('saliendo');
                setTimeout(() => nota.remove(), 350);
            };
            cerrar.addEventListener('click', quitar);
            nota.append(simbolo, enlace, cerrar);
            zona.appendChild(nota);
            setTimeout(quitar, 8000);
        };

        if ('EventSource' in window) {
            let fuente = null;
            const abrir = () => {
                if (fuente) return;
                fuente = new EventSource('/avisos/flujo');
                fuente.addEventListener('avisos', (e) => {
                    let avisos;
                    try { avisos = JSON.parse(e.data); } catch { return; }
                    const antes = new Set([...campana.querySelectorAll('[data-aviso]')].map((i) => i.dataset.aviso));
                    pintarAvisos(avisos);
                    actualizarCuenta();
                    const recien = avisos.filter((a) => !antes.has(a.clave));
                    if (recien.length) {
                        // Vuelve a lanzar el balanceo de la campana
                        campana.classList.remove('con-nuevos');
                        void campana.offsetWidth;
                        campana.classList.add('con-nuevos');
                        recien.forEach(notificar);
                    }
                });
            };
            const cerrarFuente = () => {
                if (fuente) fuente.close();
                fuente = null;
            };
            // Solo con la pestaña a la vista: el navegador admite pocas conexiones abiertas a la vez por web
            document.addEventListener('visibilitychange', () => (document.hidden ? cerrarFuente() : abrir()));
            window.addEventListener('pagehide', cerrarFuente);
            if (!document.hidden) abrir();
        }
    }

    // --- Buscador con sugerencias: <form data-sugerencias> con un <input name="q">. Mientras se escribe pide
    // /libros/sugerencias (JSON) y enseña libros y autores en una lista accesible (combobox de ARIA): flechas para
    // moverse, Intro para abrir, Escape para cerrar. Los datos se pintan con textContent, nunca como HTML ---
    const sinTildes = (texto) => texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
    const resaltar = (destino, texto, buscado) => {
        const normal = sinTildes(texto);
        const desde = normal.length === texto.length ? normal.indexOf(sinTildes(buscado)) : -1;
        if (desde < 0) {
            destino.textContent = texto;
            return;
        }
        const marca = document.createElement('mark');
        marca.textContent = texto.slice(desde, desde + buscado.length);
        destino.append(texto.slice(0, desde), marca, texto.slice(desde + buscado.length));
    };
    document.querySelectorAll('form[data-sugerencias]').forEach((formulario, n) => {
        const campo = formulario.querySelector('input[name="q"]');
        if (!campo) return;
        const lista = document.createElement('div');
        lista.className = 'sugerencias';
        lista.id = `sugerencias-${n}`;
        lista.setAttribute('role', 'listbox');
        lista.setAttribute('aria-label', 'Sugerencias del catálogo');
        lista.hidden = true;
        formulario.appendChild(lista);
        formulario.classList.add('con-sugerencias');
        campo.setAttribute('role', 'combobox');
        campo.setAttribute('aria-autocomplete', 'list');
        campo.setAttribute('aria-expanded', 'false');
        campo.setAttribute('aria-controls', lista.id);
        campo.setAttribute('autocomplete', 'off');

        let opciones = [];
        let activa = -1;
        let pedido = 0;
        let espera = 0;
        const cerrar = () => {
            lista.hidden = true;
            campo.setAttribute('aria-expanded', 'false');
            campo.removeAttribute('aria-activedescendant');
            activa = -1;
        };
        const marcar = (i) => {
            opciones.forEach((op, j) => {
                op.classList.toggle('activa', j === i);
                op.setAttribute('aria-selected', String(j === i));
            });
            activa = i;
            if (i >= 0) {
                campo.setAttribute('aria-activedescendant', opciones[i].id);
                opciones[i].scrollIntoView({ block: 'nearest' });
            } else campo.removeAttribute('aria-activedescendant');
        };
        const opcion = (href, clase) => {
            const enlace = document.createElement('a');
            enlace.href = href;
            enlace.className = `sugerencia ${clase}`;
            enlace.setAttribute('role', 'option');
            enlace.id = `${lista.id}-${opciones.length}`;
            enlace.tabIndex = -1;
            opciones.push(enlace);
            return enlace;
        };
        const grupo = (titulo) => {
            const p = document.createElement('p');
            p.className = 'sugerencias-grupo';
            p.setAttribute('role', 'presentation');
            p.textContent = titulo;
            lista.appendChild(p);
        };
        const pintar = (datos, texto) => {
            lista.replaceChildren();
            opciones = [];
            if (datos.libros.length) grupo('Libros');
            datos.libros.forEach((libro) => {
                const op = opcion(`/libros/${libro.id}`, 'sugerencia-libro');
                const lomo = document.createElement('span');
                lomo.className = `lomo lomo-mini tono-${libro.tono}`;
                lomo.setAttribute('aria-hidden', 'true');
                const datosLibro = document.createElement('span');
                datosLibro.className = 'sugerencia-datos';
                const titulo = document.createElement('strong');
                resaltar(titulo, libro.titulo, texto);
                const detalle = document.createElement('small');
                resaltar(detalle, [libro.autores, libro.anio].filter(Boolean).join(' · '), texto);
                datosLibro.append(titulo, detalle);
                const estado = document.createElement('span');
                estado.className = `disponibilidad ${libro.disponibles > 0 ? 'si' : 'no'}`;
                estado.textContent = libro.disponibles > 0 ? 'Disponible' : 'Prestado';
                op.append(lomo, datosLibro, estado);
                lista.appendChild(op);
            });
            if (datos.autores.length) grupo('Autores');
            datos.autores.forEach((autor) => {
                const op = opcion(`/autores/${autor.id}`, 'sugerencia-autor');
                const icono = document.createElement('span');
                icono.className = 'sugerencia-icono';
                icono.setAttribute('aria-hidden', 'true');
                icono.innerHTML = '<i class="ti ti-feather"></i>';
                const datosAutor = document.createElement('span');
                datosAutor.className = 'sugerencia-datos';
                const nombre = document.createElement('strong');
                resaltar(nombre, autor.nombre, texto);
                const detalle = document.createElement('small');
                detalle.textContent = [autor.nacionalidad, autor.libros === 1 ? '1 libro' : `${autor.libros} libros`]
                    .filter(Boolean).join(' · ');
                datosAutor.append(nombre, detalle);
                op.append(icono, datosAutor);
                lista.appendChild(op);
            });
            if (!datos.libros.length && !datos.autores.length) {
                const vacio = document.createElement('p');
                vacio.className = 'sugerencias-vacio';
                vacio.textContent = `Nada coincide con «${texto}».`;
                lista.appendChild(vacio);
            }
            const todos = opcion(`${formulario.action}?q=${encodeURIComponent(texto)}`, 'sugerencia-todos');
            todos.append('Ver todos los resultados de ');
            const fuerte = document.createElement('strong');
            fuerte.textContent = `«${texto}»`;
            todos.append(fuerte);
            lista.appendChild(todos);
            lista.hidden = false;
            campo.setAttribute('aria-expanded', 'true');
            marcar(-1);
        };
        campo.addEventListener('input', () => {
            clearTimeout(espera);
            const texto = campo.value.trim();
            if (texto.length < 2) {
                pedido++;
                cerrar();
                return;
            }
            espera = setTimeout(async () => {
                const mio = ++pedido;
                try {
                    const respuesta = await fetch(`/libros/sugerencias?q=${encodeURIComponent(texto)}`,
                        { headers: { Accept: 'application/json' } });
                    if (!respuesta.ok) return;
                    const datos = await respuesta.json();
                    if (mio === pedido) pintar(datos, texto);
                } catch { /* sin conexión: el buscador sigue funcionando sin sugerencias */ }
            }, 140);
        });
        campo.addEventListener('keydown', (e) => {
            if (lista.hidden || !opciones.length) return;
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                marcar((activa + 1) % opciones.length);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                marcar(activa <= 0 ? opciones.length - 1 : activa - 1);
            } else if (e.key === 'Enter' && activa >= 0) {
                e.preventDefault();
                location.href = opciones[activa].href;
            } else if (e.key === 'Escape') {
                cerrar();
            }
        });
        campo.addEventListener('focus', () => {
            if (opciones.length && campo.value.trim().length >= 2) {
                lista.hidden = false;
                campo.setAttribute('aria-expanded', 'true');
            }
        });
        document.addEventListener('pointerdown', (e) => {
            if (!formulario.contains(e.target)) cerrar();
        });
        formulario.addEventListener('focusout', (e) => {
            if (!formulario.contains(e.relatedTarget)) cerrar();
        });
    });

    // --- Al pedir o reservar, el libro «vuela» hasta Mis préstamos o Mis reservas del menú: <form data-volar="mis-prestamos">.
    // Después el formulario se envía con normalidad; en la página siguiente, esa entrada del menú se ilumina.
    // Sin GSAP o con «reducir movimiento», se envía directamente ---
    const llegada = (() => { try { return sessionStorage.getItem('backlord-llegada'); } catch { return null; } })();
    if (llegada) {
        try { sessionStorage.removeItem('backlord-llegada'); } catch { /* sin almacenamiento */ }
        document.querySelector(`.menu-enlace[href="${llegada}"]`)?.classList.add('llega');
    }
    document.querySelectorAll('form[data-volar]').forEach((formulario) => {
        formulario.addEventListener('submit', (e) => {
            if (reducido || !gsap || formulario.dataset.volando === 'si') return;
            const ruta = formulario.dataset.volar === 'mis-reservas' ? '/user/reservas' : '/user/prestamos';
            const enlace = document.querySelector(`.menu-enlace[href="${ruta}"]`);
            // En el móvil el menú está plegado: el libro va hacia el botón de la cuenta
            const destino = enlace && enlace.offsetParent ? enlace : document.querySelector('.cabecera .perfil');
            const origen = formulario.closest('.estante-libro')?.querySelector('.cubierta-libro')
                || formulario.closest('li')?.querySelector('.lomo')
                || document.querySelector('.ficha-portada .cubierta-libro')
                || formulario.querySelector('button');
            if (!destino || !origen) return;
            e.preventDefault();
            try { sessionStorage.setItem('backlord-llegada', ruta); } catch { /* sin almacenamiento */ }
            const desde = origen.getBoundingClientRect();
            const hasta = destino.getBoundingClientRect();
            const copia = origen.cloneNode(true);
            copia.classList.add('libro-volador');
            copia.setAttribute('aria-hidden', 'true');
            Object.assign(copia.style, { left: `${desde.left}px`, top: `${desde.top}px`, width: `${desde.width}px`, height: `${desde.height}px` });
            document.body.appendChild(copia);
            const enviar = () => {
                formulario.dataset.volando = 'si';
                formulario.requestSubmit();
            };
            gsap.timeline({ onComplete: enviar })
                .to(copia, { y: -40, scale: 1.06, rotation: -4, duration: 0.22, ease: 'power2.out' })
                .to(copia, {
                    x: hasta.left + hasta.width / 2 - (desde.left + desde.width / 2),
                    y: hasta.top + hasta.height / 2 - (desde.top + desde.height / 2),
                    scale: Math.max(0.08, 28 / Math.max(desde.width, desde.height)),
                    rotation: -18, opacity: 0.25, duration: 0.6, ease: 'power3.in'
                })
                .add(() => destino.classList.add('recibe'));
        });
    });

    // --- «Tu año lector»: historia de pantallas (<section data-historia> con <article class="diapositiva">).
    // Cada pantalla dura lo que tarda en llenarse su barra (animación CSS): al terminar pasa a la siguiente.
    // Clic o toque en el tercio izquierdo vuelve atrás y en el resto avanza; flechas del teclado; espacio o el
    // botón para pausar; mantener pulsado también pausa. Con «reducir movimiento» no avanza sola ---
    const historia = document.querySelector('[data-historia]');
    if (historia) {
        const pantallas = [...historia.querySelectorAll('.diapositiva')];
        const barras = historia.querySelector('.historia-barras');
        const botonPausa = historia.querySelector('[data-historia-pausa]');
        pantallas.forEach(() => {
            const barra = document.createElement('span');
            barra.className = 'barra';
            const relleno = document.createElement('span');
            relleno.className = 'relleno';
            barra.appendChild(relleno);
            barras.appendChild(barra);
        });
        let actual = -1;
        let pausaManual = false;
        const entrada = (pantalla) => {
            pantalla.querySelectorAll('[data-cuenta]').forEach((cifra) => {
                if (!cifra.dataset.final) cifra.dataset.final = cifra.textContent.trim();
                const final = parseInt(cifra.dataset.final, 10);
                if (!gsap || reducido || Number.isNaN(final)) return;
                const estado = { valor: 0 };
                cifra.textContent = '0';
                gsap.to(estado, {
                    valor: final, duration: 1.5, ease: 'power3.out', delay: 0.35,
                    onUpdate: () => { cifra.textContent = String(Math.round(estado.valor)); }
                });
            });
            if (gsap && !reducido) {
                gsap.fromTo(pantalla.querySelectorAll('[data-entra]'),
                    { y: 30, opacity: 0, scale: 0.96 },
                    { y: 0, opacity: 1, scale: 1, duration: 0.7, ease: 'power3.out', stagger: 0.11, overwrite: true });
            }
        };
        const mostrar = (indice) => {
            const i = Math.max(0, Math.min(pantallas.length - 1, indice));
            actual = i;
            pantallas.forEach((p, j) => {
                p.classList.toggle('activa', j === i);
                p.inert = j !== i;
            });
            [...barras.children].forEach((barra, j) => {
                barra.classList.toggle('hecha', j < i || (j === i && (reducido || i === pantallas.length - 1)));
                barra.classList.remove('actual');
            });
            const barra = barras.children[i];
            void barra.offsetWidth; // reinicia la animación de la barra
            if (!reducido && i < pantallas.length - 1) barra.classList.add('actual');
            entrada(pantallas[i]);
        };
        [...barras.querySelectorAll('.relleno')].forEach((relleno, j) => {
            relleno.addEventListener('animationend', () => { if (j === actual) mostrar(actual + 1); });
        });
        const pausar = (si) => {
            historia.classList.toggle('pausada', si);
            if (botonPausa) {
                botonPausa.setAttribute('aria-label', si ? 'Seguir con la historia' : 'Pausar la historia');
                botonPausa.querySelector('i').className = si ? 'ti ti-player-play' : 'ti ti-player-pause';
            }
        };
        botonPausa?.addEventListener('click', (e) => {
            e.stopPropagation();
            pausaManual = !historia.classList.contains('pausada');
            pausar(pausaManual);
        });
        // Mantener pulsado pausa; soltar sigue. Una pulsación larga no cuenta como clic para avanzar
        let pulsadoDesde = 0;
        historia.addEventListener('pointerdown', (e) => {
            if (e.target.closest('a, button')) return;
            pulsadoDesde = performance.now();
            pausar(true);
        });
        historia.addEventListener('pointerup', () => { if (!pausaManual) pausar(false); });
        historia.addEventListener('pointercancel', () => { if (!pausaManual) pausar(false); });
        historia.addEventListener('click', (e) => {
            if (e.target.closest('a, button') || performance.now() - pulsadoDesde > 300) return;
            const caja = historia.getBoundingClientRect();
            mostrar(e.clientX - caja.left < caja.width / 3 ? actual - 1 : actual + 1);
        });
        document.addEventListener('keydown', (e) => {
            if (e.target.closest('input, textarea, select')) return;
            if (e.key === 'ArrowRight') mostrar(actual + 1);
            else if (e.key === 'ArrowLeft') mostrar(actual - 1);
            else if (e.key === ' ' && !e.target.closest('button, a')) {
                e.preventDefault();
                pausaManual = !historia.classList.contains('pausada');
                pausar(pausaManual);
            }
        });
        historia.querySelector('[data-historia-repetir]')?.addEventListener('click', (e) => {
            e.stopPropagation();
            pausaManual = false;
            pausar(false);
            mostrar(0);
        });
        mostrar(0);
    }

    // --- Medidor de fuerza de la contraseña: <div data-fuerza-de="id-del-campo"> (fragmento fuerzaClave).
    // Orienta mientras se escribe; la regla que manda es la del servidor (UsuarioService.problemaPassword) ---
    const nivelesClave = ['', 'Muy débil', 'Débil', 'Aceptable', 'Fuerte', 'Muy fuerte'];
    const fuerzaDe = (clave) => {
        if (!clave) return 0;
        let puntos = 0;
        if (clave.length >= 8) puntos++;
        if (clave.length >= 12) puntos++;
        if (/\p{Ll}/u.test(clave) && /\p{Lu}/u.test(clave)) puntos++;
        if (/\d/.test(clave)) puntos++;
        if (/[^\p{L}\d]/u.test(clave)) puntos++;
        // Repeticiones y comienzos típicos valen poco aunque sean largos
        if (/^(.)\1+$/u.test(clave) || /^(1234|abcd|qwerty|password|contrase|usuario|admin|biblioteca)/i.test(clave)) {
            puntos = Math.min(puntos, 1);
        }
        return clave.length < 8 ? Math.min(puntos, 1) : Math.max(1, puntos);
    };
    document.querySelectorAll('[data-fuerza-de]').forEach((medidor) => {
        const campo = document.getElementById(medidor.dataset.fuerzaDe);
        if (!campo) return;
        const texto = medidor.querySelector('.fuerza-texto');
        const inicial = texto.textContent;
        campo.addEventListener('input', () => {
            const clave = campo.value;
            const nivel = fuerzaDe(clave);
            medidor.dataset.nivel = String(nivel);
            if (!clave) texto.textContent = inicial;
            else if (clave.length < 8) {
                const faltan = 8 - clave.length;
                texto.textContent = `Te ${faltan === 1 ? 'falta 1 carácter' : `faltan ${faltan} caracteres`}`;
            } else texto.textContent = `Seguridad: ${nivelesClave[nivel].toLowerCase()}`;
        });
    });

    // --- Confirmación de la contraseña: <input data-igual-que="id"> y <p data-coincide-de="id-de-este-campo"> ---
    document.querySelectorAll('[data-igual-que]').forEach((campo) => {
        const original = document.getElementById(campo.dataset.igualQue);
        const aviso = document.querySelector(`[data-coincide-de="${campo.id}"]`);
        if (!original || !aviso) return;
        const comprobar = () => {
            aviso.hidden = !campo.value;
            const iguales = campo.value === original.value;
            aviso.classList.toggle('si', iguales);
            aviso.innerHTML = '';
            const icono = document.createElement('i');
            icono.className = iguales ? 'ti ti-circle-check' : 'ti ti-circle-x';
            aviso.append(icono, iguales ? ' Coinciden' : ' Todavía no coinciden');
        };
        campo.addEventListener('input', comprobar);
        original.addEventListener('input', () => { if (campo.value) comprobar(); });
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
    // (en la portada ya vista no: debe aparecer al instante)
    if (!portadaYaVista) {
        gsap.from('body > main', { opacity: 0, duration: 0.45, ease: 'power2.out', clearProps: 'opacity' });
    }

    // Solo se anima lo que existe en la página: así la consola no se llena de avisos
    const hay = (selector) => document.querySelector(selector) !== null;

    // Entrada del título de la portada: solo la primera vez de la sesión
    if (hay('.titulo-3d .letra') && !portadaYaVista) {
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

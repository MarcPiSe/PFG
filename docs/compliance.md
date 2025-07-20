# Checklist de desplegament segur (compliment bàsic RGPD / LSSICE)

- [ ] HTTPS (TLS 1.3) amb Let's Encrypt o certificat propi.
- [ ] Proxy invers amb cabeceres:
  - `Strict-Transport-Security: max-age=63072000; includeSubDomains`
  - `Content-Security-Policy: default-src 'self'`
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: strict-origin-when-cross-origin`
- [ ] Tokens JWT servits en cookie `HttpOnly; Secure; SameSite=Lax` (opcional mode dual mentre es manté localStorage).
- [ ] Contrasenyes xifrades amb BCrypt (config per defecte OK).
- [ ] Còpia de seguretat xifrada: `pg_dump | gpg --symmetric AES256` (cron diari).
- [ ] Rotació i retenció de logs ≤ 6 mesos.
- [ ] Usuari administrador amb 2FA (TOTP) opcional.
- [ ] Escaneig de vulnerabilitats (Trivy, OWASP ZAP) abans de publicar imatge Docker.
- [ ] Revisió anual del registre d'activitats (vegeu `docs/rgpd-auto-evaluacion.md`).

*Plantilla inspirada en Reial Decret 311/2022 (ENS bàsic).*  
https://www.boe.es/buscar/act.php?id=BOE-A-2022-14698 

## Altres normatives a considerar

Aquestes són legislacions addicionals que poden afectar l'ús de l'aplicació si es vol explotar a nivell comercial, especialment en cas de prestar serveis a consumidors o incloure contingut publicitari. No totes són d'aplicació obligatòria en cada cas, però és recomanable revisar-les segons la finalitat i l'ús previst.

| Norma | Àmbit principal |
|-------|------------------|
| Llei General de Publicitat | Comunicacions comercials del lloc web, campanyes de captació i newsletters |
| Llei de Propietat Intel·lectual | Protegeix el codi font, la documentació i els continguts pujats pels usuaris. |
| Llei de Marques | Regula l'ús del logotip i nom del projecte per evitar confusions. |
| Llei 7/1998 sobre Condicions Generals de la Contractació | Obliga a disposar de Termes i Condicions clares quan es presta el servei al públic. |
| Llei de Competència Deslleial | Garanteix pràctiques comercials honestes en la distribució del programari. |

> Avís: aquestes normes complementen RGPD, LOPDGDD, LSSICE i ENS. Correspon a l'operador final analitzar la seva aplicació concreta i incorporar les clàusules o processos necessaris abans de llançar el servei en producció. 

> Avís: aquestes normes complementen RGPD, LOPDGDD, LSSICE i ENS. Correspon a l’operador final analitzar la seva aplicació concreta i incorporar les clàusules o processos necessaris abans de llançar el servei en producció. 
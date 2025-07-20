## Casos de Prova del Sistema

Aquest document detalla els casos de prova funcionals per a l'aplicació web, servidor i aplicació d'escriptori (Tauri), cobrint els fluxos d'èxit i d'error per a cada característica principal. Cada cas inclou una descripció de l'escenari, el resultat esperat a les interfícies (web i desktop) i els canvis corresponents a la base de dades.

---

## UC-01 i UC-02: Gestió de Sessions

### Cas de Prova 1: Registre d'Usuari

#### Cas d'Èxit: Registre amb dades vàlides a la interfície web
- [ ] **Resum:** Un usuari nou es registra a través del formulari web amb dades correctes i úniques.
- [ ] **Endpoint:** `POST /users/auth/register`
- [ ] **Validacions específiques del servidor:**
    - [ ] **Username:** Pattern `^[a-zA-Z0-9_]+$`, longitud 3-50 caràcters
    - [ ] **Password:** Pattern `^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$`, mínim 8 caràcters
    - [ ] **Email:** Pattern `^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$`
    - [ ] **Noms:** FirstName i LastName mínims 2 caràcters, màxims 100
- [ ] **Procés de registre transaccional complet:**
    1. UserAuthentication verifica disponibilitat username i email
    2. Es crea `UserEntity` amb password encriptat (BCrypt) i rol USER
    3. **Feign call:** FileManagement.createRoot() - crea carpeta arrel
    4. **Feign call:** SyncService.addRoot() - registra al servei de sincronització
    5. **Feign call:** UserManagement.createUser() - crea perfil d'usuari
    6. Si qualsevol pas falla → rollback automàtic eliminant UserEntity
- [ ] **Procés a la UI:**
    1. L'usuari completa el formulari de registre amb validacions en temps real
    2. El toggle de signup/login està activat per mostrar el formulari de registre
    3. Es validen els camps de forma reactiva (username, email, password, confirmPassword)
    4. S'envia la petició al servidor en prémer "Register"
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna `201 Created` amb tokens d'accés i de refresc amb headers:
        - `Authorization: Bearer {accessToken}` (validesa: 15 minuts)
        - `X-Refresh-Token: {refreshToken}` (validesa: 24 hores)
    - [ ] **Tokens JWT contenen:**
        - Claims: role, connection-id (UUID), password-changed-at (timestamp)
        - Signats amb clau privada RSA
    - [ ] **UI:** 
        - Es guarden els tokens a `localStorage` (accessToken, refreshToken, username)
        - Es carrega la informació de l'usuari amb `userService.getCurrentUser()`
        - Es connecta automàticament el WebSocket per sincronització
        - Es redirigeix a la pàgina principal (`FileManager`)
        - Es mostra notificació d'èxit
    - [ ] **Base de dades (transaccional):**
        - [ ] **UserAuthentication:** Nova entrada a `user_entity` amb password BCrypt i `lastPasswordChange`
        - [ ] **UserManagement:** Nova entrada a `user_info` amb perfil complet
        - [ ] **FileManagement:** Creada carpeta arrel a `folder_entity` i `element_entity`
        - [ ] **FileAccessControl:** Assignat permís de propietari (ADMIN) a `access_rule`
        - [ ] **SyncService:** Registrat root folder per sincronització
    - [ ] **Sincronització:** WebSocket activat per rebre actualitzacions en temps real

#### Cas de Fallada: Validació específica de camps
- [ ] **Resum:** Validacions detallades per cada camp segons patterns del servidor.
- [ ] **Validacions Username:**
    - Pattern invàlid (caràcters especials): `400 Bad Request`
    - Longitud < 3 o > 50: `400 Bad Request`  
    - Username existent: `409 Conflict` "Username is already taken"
- [ ] **Validacions Password:**
    - Longitud < 8: `400 Bad Request` "Password must be at least 8 characters long"
    - Sense majúscula/minúscula/número: `400 Bad Request` "Password must contain..."
- [ ] **Validacions Email:**
    - Format invàlid: `400 Bad Request` "Invalid email format"
    - Email existent: `409 Conflict` "Email is already taken"
- [ ] **Validacions FirstName/LastName:**
    - Buit: `400 Bad Request` "First name cannot be empty"
    - Longitud < 2 o > 100: `400 Bad Request`

#### Cas de Fallada: Error en creació de carpeta arrel
- [ ] **Resum:** FileManagement falla en crear carpeta arrel durant registre.
- [ ] **Resultat Esperat:**
    - [ ] **API:** `500 Internal Server Error` "Root not created"
    - [ ] **Rollback:** UserEntity eliminat automàticament de user_entity
    - [ ] **Estado:** Cap entrada persistent a cap servei

#### Cas de Fallada: Dades invàlides o duplicades a la interfície web
- [ ] **Resum:** Intent de registre amb dades que no compleixen les validacions.
- [ ] **Resultat Esperat:**
    - [ ] **UI (Validació Frontend):**
        - Validació en temps real dels camps (format email, longitud password)
        - Missatges d'error sota cada camp invàlid
        - Botó de registre deshabilitat fins que tots els camps siguin vàlids
        - No es fa la petició al servidor si hi ha errors de validació
    - [ ] **Backend (si passa validació frontend):**
        *   Retorna `400 Bad Request` per a dades amb format incorrecte
        *   Retorna `409 Conflict` si el nom d'usuari o l'email ja existeixen
    - [ ] **UI (Error del servidor):**
        - Es mostra toast d'error amb el missatge apropiat
        - Els camps mantenen els valors introduïts
        - No es redirigeix ni es canvia l'estat d'autenticació
    - [ ] **Base de dades:** No es crea cap registre nou

---

### Cas de Prova 2: Inici de Sessió a la Interfície Web

#### Cas d'Èxit: Credencials vàlides
- [ ] **Resum:** Un usuari existent inicia sessió amb les seves credencials correctes.
- [ ] **Endpoint:** `POST /users/auth/login`
- [ ] **Procés de validació del servidor:**
    1. UserService.findByUsername() busca usuari amb case-insensitive
    2. BCryptPasswordEncoder.matches() verifica password contra hash guardat
    3. JwtTokenUtil.generateToken() crea tokens amb claims específics
- [ ] **Procés a la UI:**
    1. L'usuari introdueix username i password
    2. Es validen els camps localment
    3. S'envia la petició de login
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna `200 OK` amb headers:
        - `Authorization: Bearer {accessToken}` (15 min validesa)
        - `X-Refresh-Token: {refreshToken}` (24h validesa)
    - [ ] **Claims JWT:**
        - username (subject), role, connection-id, password-changed-at
        - Issuer/Expiration timestamps, signatura RSA
    - [ ] **UI:**
        - Es guarden els tokens a `localStorage`
        - Es carrega la informació de l'usuari
        - Es connecta el WebSocket automàticament
        - Es carrega l'estat inicial de fitxers amb `fetchInitialState()`
        - Es redirigeix al FileManager
        - Es mostra el loader fins que es carreguin les dades inicials
    - [ ] **Base de dades:** Operació de només lectura sobre `user_entity`
    - [ ] **Sincronització:** Connexió WebSocket establerta per rebre notificacions

#### Cas de Fallada: Credencials incorrectes
- [ ] **Resum:** Intent d'inici de sessió amb credencials incorrectes.
- [ ] **Endpoint:** `POST /users/auth/login`
- [ ] **Validacions del servidor:**
    - Username no existeix: UserRepository.findByUsernameIgnoreCase() retorna empty
    - Password incorrecte: BCryptPasswordEncoder.matches() retorna false
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna `401 Unauthorized` "Invalid credentials"
    - [ ] **UI:**
        - Es mostra toast d'error "Invalid credentials"
        - Els camps es mantenen omplerts
        - No es canvia l'estat d'autenticació
        - Es manté a la pàgina de login
    - [ ] **Base de dades:** No es produeix cap modificació

---

### Cas de Prova 2B: Inici de Sessió a l'Aplicació Desktop (Tauri)

#### Cas d'Èxit: Credencials vàlides des de l'aplicació desktop
- [ ] **Resum:** Un usuari existent inicia sessió amb les seves credencials correctes des de l'aplicació Tauri.
- [ ] **Endpoint:** `POST /users/auth/login` (mateix que web)
- [ ] **Procés a l'Aplicació Desktop:**
    1. L'aplicació Tauri s'inicia i detecta que no hi ha tokens vàlids
    2. Es mostra la finestra de login (`/login`)
    3. L'usuari introdueix username i password
    4. Es crida la funció `login(username, password)` de Tauri
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna `200 OK` amb headers `authorization` i `x-refresh-token`
    - [ ] **Aplicació Desktop:**
        - Es creen objectes `Token` amb els valors i `SystemTime::now()`
        - S'actualitza el `CONFIG` global amb tokens, username i password
        - Es crida `update_config()` per persistir la configuració a `config.json`
        - Es tanca la finestra de login
        - S'inicia `token::watch_tokens()` per gestió automàtica de tokens
        - S'obre la finestra principal (`open_main_window()`)
        - S'inicia el `synchronizer::start()` per sincronització de fitxers
    - [ ] **Base de dades:** Operació de només lectura sobre `user_entity`
    - [ ] **Sincronització Desktop:** 
        - Connexió WebSocket a `/websocket` amb token d'autorització
        - Inici del file watcher per detectar canvis locals
        - Construcció de l'arbre local de fitxers (`fstree::build_tree()`)

#### Cas de Fallada: Credencials incorrectes des de desktop
- [ ] **Resum:** Intent d'inici de sessió amb credencials incorrectes.
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna status diferent de success
    - [ ] **Aplicació Desktop:**
        - Es retorna `Err("Username or password incorrect")`
        - La finestra de login es manté oberta
        - Es mostra l'error a la interfície
        - No s'actualitza la configuració ni s'inicien serveis
    - [ ] **Base de dades:** No es produeix cap modificació

---

### Cas de Prova 3: Refresc Automàtic de Token (Interceptor)

#### Cas d'Èxit: Token expirat amb refresh vàlid
- [ ] **Resum:** Una petició falla per token expirat i s'intenta renovar automàticament.
- [ ] **Endpoint:** `POST /users/auth/keep-alive`
- [ ] **Procés de validació del servidor:**
    1. JwtTokenUtil.validateTokenExpiration() verifica expiració refresh token
    2. JwtTokenUtil.extractUsername() extreu username del token
    3. UserService.loadUserByUsername() carrega usuari
    4. Es generen nous tokens amb nous connection-id i timestamps
- [ ] **Procés:**
    1. Una petició autenticada retorna `401 Unauthorized`
    2. L'interceptor detecta l'error i intenta renovar el token
    3. S'envia el refresh token al servidor amb header `X-Refresh-Token`
    4. Es reben nous tokens i es reintenta la petició original
- [ ] **Resultat Esperat:**
    - [ ] **API:** Refresc retorna nous tokens amb headers Authorization i X-Refresh-Token
    - [ ] **Nous tokens contenen:**
        - Nous timestamps d'emissió i expiració
        - Nou connection-id (UUID)
        - Mateix username i rol mantinguts
    - [ ] **UI:** L'operació es completa transparentment sense que l'usuari se n'adoni
    - [ ] **Base de dades:** No es produeix cap modificació per al refresc

#### Cas de Fallada: Refresh token invàlid o expirat
- [ ] **Resum:** Intent de refresc amb token de refresc expirat o invàlid.
- [ ] **Validacions del servidor:**
    - Token expirat: JwtTokenUtil.validateTokenExpiration() retorna false
    - Token corromput: JwtTokenUtil.extractUsername() retorna null
    - Usuari no existeix: UserService.loadUserByUsername() llança exception
- [ ] **Resultat Esperat:**
    - [ ] **API:** Endpoint de refresc retorna `401 Unauthorized`
    - [ ] **UI:**
        - Es neteja el localStorage (tokens, username)
        - Es desconnecta el WebSocket
        - Es redirigeix a `/login`
        - Es reinicia l'estat global de l'aplicació

#### Cas d'Èxit: Gestió automàtica de tokens a l'aplicació desktop
- [ ] **Resum:** L'aplicació Tauri gestiona automàticament l'expiració de tokens.
- [ ] **Procés a l'Aplicació Desktop:**
    1. `token::watch_tokens()` monitoritza els temps d'expiració
    2. Quan el refresh token està a punt d'expirar (24h), es prepara per logout
    3. Si el token d'accés expira (15 min), el servei intenta renovar-lo
- [ ] **Resultat Esperat:**
    - [ ] **Desktop (Expiry automàtic):**
        - Quan el refresh token expira, es crida automàticament `logout()`
        - Es netegen tokens del `CONFIG`
        - Es para el synchronizer i token watcher
        - Es tanquen totes les finestres excepte login
        - Es mostra la finestra de login
    - [ ] **Desktop (Renovació):** Token d'accés es renova automàticament sense intervenció

---

### Cas de Prova 4: Tancar Sessió

#### Cas d'Èxit: Logout des de la interfície
- [ ] **Resum:** Un usuari autenticat fa clic al botó de tancar sessió.
- [ ] **Endpoint:** `POST /users/auth/logout` (opcional, es fa cleanup local independentment)
- [ ] **Procés a la UI:**
    1. L'usuari fa clic al botó de logout al header
    2. Es crida `logoutEndpoint()` del hook d'autenticació
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - Es neteja el localStorage (accessToken, refreshToken, userId, username)
        - Es desconnecta el WebSocket amb `websocketService.disconnect()`
        - Es reinicia l'estat global (Zustand stores)
        - Es fa `window.location.href = "/login"` per redirigir
    - [ ] **Backend:** Token invalidat si s'implementa llista negra

#### Cas d'Èxit: Logout des de l'aplicació desktop
- [ ] **Resum:** Un usuari autenticat fa logout des de l'aplicació Tauri.
- [ ] **Procés a l'Aplicació Desktop:**
    1. L'usuari fa clic al botó "logout" a la configuració d'usuari
    2. Es crida la funció Tauri `logout(app_handle)`
- [ ] **Resultat Esperat:**
    - [ ] **Aplicació Desktop:**
        - Es netegen els tokens del `CONFIG` (`config.token.take()`, `config.refresh_token.take()`)
        - Es crida `update_config()` per persistir els canvis a `config.json`
        - Es para el synchronizer amb `synchronizer::stop()`
        - Es para el token watcher amb `token::stop()`
        - Es tanquen totes les finestres excepte "Login"
        - Es mostra la finestra de login amb `open_login_window()`
    - [ ] **Sistema de fitxers:** Configuració actualitzada sense tokens

---

### Cas de Prova 4B: Tancar Sessió des de l'Aplicació Desktop

#### Cas d'Èxit: Logout des de l'aplicació desktop
- [ ] **Resum:** Un usuari autenticat fa logout des de l'aplicació Tauri.
- [ ] **Procés a l'Aplicació Desktop:**
    1. L'usuari fa clic al botó "logout" a la configuració d'usuari
    2. Es crida la funció Tauri `logout(app_handle)`
- [ ] **Resultat Esperat:**
    - [ ] **Aplicació Desktop:**
        - Es netegen els tokens del `CONFIG` (`config.token.take()`, `config.refresh_token.take()`)
        - Es crida `update_config()` per persistir els canvis a `config.json`
        - Es para el synchronizer amb `synchronizer::stop()`
        - Es para el token watcher amb `token::stop()`
        - Es tanquen totes les finestres excepte "Login"
        - Es mostra la finestra de login amb `open_login_window()`
    - [ ] **Sistema de fitxers:** Configuració actualitzada sense tokens

---

## Cas de Prova 5: Configuració Inicial de l'Aplicació Desktop

### Cas d'Èxit: Primera configuració de l'aplicació Tauri
- [ ] **Resum:** Un usuari configura per primera vegada l'aplicació desktop amb servidor i carpeta local.
- [ ] **Procés a l'Aplicació Desktop:**
    1. L'aplicació detecta que `config.is_configured = false`
    2. Es mostra la finestra de configuració inicial (`/initialConfiguration`)
    3. L'usuari introdueix URL del servidor i selecciona carpeta local
    4. Es crida `save_initial_config(server_url, folder_path)`
- [ ] **Validacions realitzades:**
    - [ ] **Servidor:** Petició a `{server_url}/actuator/health` per verificar connectivitat
    - [ ] **Carpeta:** Verificació que el path existeix i és un directori
- [ ] **Resultat Esperat:**
    - [ ] **Configuració vàlida:**
        - S'actualitza el `CONFIG` amb `server_url`, `folder_path` i `is_configured = true`
        - Es guarda la configuració a `config.json` 
        - Es tanca la finestra de configuració inicial
        - Es mostra la finestra de login
    - [ ] **Configuració invàlida:**
        - Es retorna un `HashMap<String, String>` amb errors específics
        - La finestra es manté oberta mostrant els errors
        - No es guarda cap configuració

#### Cas de Fallada: Servidor inassequible o carpeta inexistent
- [ ] **Resum:** Intent de configuració amb dades invàlides.
- [ ] **Resultat Esperat:**
    - [ ] **Servidor inassequible:** Error `"server": "server not reachable"`
    - [ ] **Carpeta inexistent:** Error `"folder": "folder does not exist"`
    - [ ] **UI Desktop:** Els errors es mostren sota els camps corresponents
    - [ ] **Configuració:** No es guarda ni s'actualitza res

---

## Cas de Prova 6: Sistema de Sincronització Desktop

### Cas d'Èxit: Sincronització bidireccional activa
- [ ] **Resum:** L'aplicació desktop manté sincronitzats els fitxers locals amb el servidor.
- [ ] **Components implicats:**
    - [ ] **WebSocket client:** Connexió a `/websocket` per rebre canvis del servidor
    - [ ] **File watcher:** `notify::RecommendedWatcher` per detectar canvis locals
    - [ ] **Debouncer:** Evita operacions massa freqüents (1000ms delay)
- [ ] **Procés de Sincronització:**
    1. Es construeix l'arbre local inicial amb `fstree::build_tree()`
    2. S'inicia el watcher recursiu sobre la carpeta configurada
    3. Es connecta al WebSocket del servidor
    4. Es processen canvis en ambdues direccions
- [ ] **Resultat Esperat:**
    - [ ] **Connexió WebSocket:**
        - `IS_CONNECTED.lock().unwrap() = true`
        - Event `is_connected: true` emès a la UI
        - Recepció de missatges `SocketResponse` amb canvis del servidor
    - [ ] **Detecció de canvis locals:**
        - Events de `notify` processats per `handle_event()`
        - Canvis enviats al servidor via API calls
        - Actualització de l'arbre local en memòria

### Cas de Fallada: Pèrdua de connexió WebSocket
- [ ] **Resum:** La connexió WebSocket es perd i l'aplicació intenta reconnectar.
- [ ] **Resultat Esperat:**
    - [ ] **Estat de connexió:**
        - `IS_CONNECTED.lock().unwrap() = false`
        - Event `is_connected: false` emès a la UI
        - UI mostra overlay de "Connection Lost"
    - [ ] **Reconnexió:**
        - Sleep de 5 segons abans de reintent
        - Loop infinit intentant reconnectar
        - Quan connecta: `IS_CONNECTED = true` i continua sincronització

---

## Cas de Prova 7: Gestió d'Operacions de Fitxers Desktop

### Cas d'Èxit: Upload automàtic de fitxer afegit localment
- [ ] **Resum:** L'usuari afegeix un fitxer a la carpeta local sincronitzada.
- [ ] **Procés a l'Aplicació Desktop:**
    1. File watcher detecta `EventKind::Create(CreateKind::File)`
    2. Es crea `Transfer` amb tipus `Upload` i estat `Active`
    3. Es construeix multipart form amb fitxer i metadades
    4. `api::upload()` envia el fitxer al servidor amb progress tracking
- [ ] **Endpoint utilitzat:** `POST /upload`
- [ ] **Resultat Esperat:**
    - [ ] **Progress tracking:** Event `transfer` emès amb progrés 0-100%
    - [ ] **API:** Fitxer pujat amb success
    - [ ] **Base de dades:**
        - Nova entrada a `element_entity` (tipus `FILE`)
        - Nova entrada a `file_entity` amb metadades (nom, mida, MIME type)
        - Nova regla de propietat a `access_rule`
    - [ ] **UI Desktop:** Transfer apareix a tab "Current transfers", després a "Completed"
    - [ ] **Sincronització:** Altres clients reben el fitxer via WebSocket

---

## UC-03, UC-15, UC-16: Gestió del Perfil d'Usuari a la Interfície Web

### Cas de Prova 5: Actualitzar Perfil des de la Configuració

#### Cas d'Èxit: Modificació de dades personals
- [ ] **Resum:** L'usuari obre la configuració i actualitza el seu nom o email.
- [ ] **Endpoint:** `PUT /users/profile`
- [ ] **Procés a la UI:**
    1. L'usuari obre el modal de configuració (`UserSettings`)
    2. Es carreguen les dades actuals amb `GET /users`
    3. L'usuari modifica camps amb validació en temps real
    4. Per username/email, es comprova disponibilitat en temps real
    5. S'activa el botó "Save" quan hi ha canvis vàlids
- [ ] **Resultat Esperat:**
    - [ ] **Validació en temps real:**
        - `GET /users/check-email` per comprovar disponibilitat d'email
        - `GET /users/check-username` per comprovar disponibilitat d'username
        - Icones de validació (check/cross/loading) al costat de cada camp
    - [ ] **API:** Retorna `200 OK` amb la informació actualitzada
    - [ ] **UI:**
        - Es mostra toast d'èxit "Profile updated successfully"
        - Es tanca el modal de configuració
        - S'actualitza l'estat de l'usuari globalment
    - [ ] **Base de dades:** S'actualitza el registre a `user_info` (`UserManagement`)

#### Cas de Fallada: Email ja existent
- [ ] **Resum:** L'usuari intenta canviar a un email ja registrat.
- [ ] **Resultat Esperat:**
    - [ ] **Validació en temps real:** La comprovació d'email mostra error abans d'enviar
    - [ ] **API (si passa validació):** Retorna `409 Conflict`
    - [ ] **UI:** Toast d'error i camp marcat com invàlid
    - [ ] **Base de dades:** No es produeix cap modificació

### Cas de Prova 6: Canviar Contrasenya

#### Cas d'Èxit: Canvi amb credencials correctes
- [ ] **Resum:** L'usuari canvia la seva contrasenya des de la configuració.
- [ ] **Endpoint:** `PUT /users/password`
- [ ] **Procés a la UI:**
    1. L'usuari omple els camps: oldPassword, newPassword, confirmPassword
    2. Validació en temps real de longitud i coincidència
    3. S'envia la petició amb les contrasenyes
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna `200 OK`
    - [ ] **UI:** Toast d'èxit i neteja dels camps de contrasenya
    - [ ] **Base de dades:** Camp `password` actualitzat a `user_entity` amb nou hash

#### Cas de Fallada: Contrasenya antiga incorrecta
- [ ] **Resum:** L'usuari introdueix una contrasenya antiga incorrecta.
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna `401 Unauthorized`
    - [ ] **UI:** Toast d'error "Current password is incorrect"

### Cas de Prova 7: Eliminar Compte Propi

#### Cas d'Èxit: Confirmació d'eliminació
- [ ] **Resum:** Un usuari elimina el seu propi compte des de la configuració.
- [ ] **Endpoint:** `DELETE /users`
- [ ] **Procés d'orquestració asíncrona completa:**
    1. **UserManagement** rep petició i verifica que no sigui SUPER_ADMIN
    2. **Fase síncrona:** UserManagement crida UserAuthentication.deleteAccountInternal()
    3. **Fase síncrona:** UserManagement elimina localment de user_info
    4. **Creació de registre d'orquestració:** Es crea UserDeletionProcess amb:
        - `fileManagementStatus: "PENDING"`
        - `fileSharingStatus: "PENDING"`
        - `fileAccessControlStatus: "PENDING"`
        - `userManagementStatus: "DONE"`
        - `userAuthenticationStatus: "DONE"`
        - `trashStatus: "PENDING"`
        - `syncServiceStatus: "PENDING"`
        - `createdAt: new Date()`
    5. **Enviament de comandos RabbitMQ:** UserManagement envia a coles:
        - `fileManagerUserDelete` → FileManagement
        - `fileSharingUserDelete` → FileSharing
        - `fileAccessUserDelete` → FileAccessControl
        - `trashUserDelete` → Trash
        - `syncUserDelete` → SyncService
- [ ] **Procés a la UI:**
    1. L'usuari fa clic al botó "Delete Account"
    2. Es mostra un diàleg de confirmació amb advertència
    3. L'usuari confirma l'eliminació
- [ ] **Procés de confirmacions asíncrones:**
    1. Cada microservei processa l'eliminació del seu domini
    2. Cada microservei envia confirmació a `user-delete-confirmation-queue`
    3. UserManagement rep confirmacions i actualitza UserDeletionProcess
    4. Quan tots els estats són "DONE", s'elimina el registre UserDeletionProcess
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna `200 OK` "User account deletion process started"
    - [ ] **UI:**
        - Es mostra toast de confirmació
        - Es neteja el localStorage
        - Es desconnecta el WebSocket
        - Es redirigeix a `/sign-up`
    - [ ] **Base de dades (orquestració asíncrona via RabbitMQ):**
        - [ ] **Immediate:** S'elimina user_entity (UserAuthentication) i user_info (UserManagement)
        - [ ] **Asíncrona:** FileManagement elimina files/folders i element_entity
        - [ ] **Asíncrona:** FileSharing elimina shared_access
        - [ ] **Asíncrona:** FileAccessControl elimina access_rule
        - [ ] **Asíncrona:** Trash elimina trash_record
        - [ ] **Asíncrona:** SyncService elimina snapshots i dades de sincronització
    - [ ] **Colas RabbitMQ utilitzades:**
        - `fileManagerUserDelete`
        - `fileSharingUserDelete`
        - `fileAccessUserDelete`
        - `trashUserDelete`
        - `syncUserDelete`
        - `user-delete-confirmation-queue` (confirmacions)
    - [ ] **Sistema de retry:** Si algún servei falla, UserDeletionService.retryPendingDeletions() reintenta automàticament

#### Cas de Fallada: Error en un microservei durant eliminació
- [ ] **Resum:** Un dels microserveis falla durant el procés d'eliminació asíncrona.
- [ ] **Resultat Esperat:**
    - [ ] **UserDeletionProcess:** Es manté amb estat "PENDING" pel servei que ha fallat
    - [ ] **Sistema de retry:** 
        - `retryPendingDeletions()` reintenta automàticament els serveis pendents
        - Es tornen a enviar comandos només als serveis amb estat "PENDING"
    - [ ] **Eventual consistency:** El procés es completa quan tots els serveis confirmen
    - [ ] **Cleanup:** `cleanupCompletedDeletions()` elimina registres quan tot està "DONE"

#### Cas d'Èxit: Verificació d'estat del procés d'eliminació
- [ ] **Resum:** Monitorització de l'estat del procés d'eliminació asíncrona.
- [ ] **Taula user_deletion_process:** Conté l'estat detallat per microservei:
    ```sql
    user_id UUID PRIMARY KEY,
    file_management_status VARCHAR(255),
    file_sharing_status VARCHAR(255),
    file_access_control_status VARCHAR(255),
    user_management_status VARCHAR(255),
    user_authentication_status VARCHAR(255),
    trash_status VARCHAR(255),
    sync_service_status VARCHAR(255),
    created_at TIMESTAMP
    ```
- [ ] **Estats possibles:** "PENDING", "DONE"
- [ ] **Query de procesos pendents:**
    ```sql
    SELECT * FROM user_deletion_process 
    WHERE file_management_status = 'PENDING' 
       OR file_sharing_status = 'PENDING' 
       OR file_access_control_status = 'PENDING'
       OR trash_status = 'PENDING'
       OR sync_service_status = 'PENDING'
    ```

---

## UC-08 a UC-12: Gestió d'Arxius i Carpetes a la Interfície Web

### Cas de Prova 8: Crear Carpeta des de la Interfície

#### Cas d'Èxit: Creació amb permisos adequats
- [ ] **Resum:** L'usuari crea una nova carpeta mitjançant el botó "+" o el menú contextual.
- [ ] **Endpoint:** `POST /files`
- [ ] **Procés de validació del servidor:**
    1. **Verificació d'accés:** FileAccessService.checkAccessElement() amb AccessType.WRITE
    2. **Validació de nom:** Verificació de caràcters prohibits i noms reservats
    3. **Creació transaccional:**
        - ElementEntity (isFolder: true)
        - FolderEntity amb metadades
        - AccessRule amb tipus ADMIN per al propietari
    4. **CommandService:** sendCreate() per sincronització WebSocket
- [ ] **Procés a la UI:**
    1. L'usuari fa clic al botó "+" (`AddButton`) o click dret → "New Folder"
    2. S'obre el diàleg `CreateNewFolderDialog`
    3. L'usuari introdueix el nom amb validació local
    4. Es valida que no existeixi el nom a la carpeta actual
- [ ] **Resultat Esperat:**
    - [ ] **Validació UI:** Es comprova que el nom no conté caràcters prohibits ni noms reservats
    - [ ] **API:** Retorna la nova carpeta creada amb FolderInfo completa
    - [ ] **Base de dades transaccional:**
        - element_entity: isFolder=true
        - folder_entity: name, userId, parentId, creationDate, lastModification
        - access_rule: userId + elementId amb ADMIN access
    - [ ] **UI:**
        - [ ] **Actualització optimista:** La carpeta apareix immediatament a la graella
        - [ ] **TanStack Query:** Actualitza la cache del directori actual i estructura de carpetes
        - [ ] **Notificació:** Toast d'èxit "Folder created successfully"
        - [ ] **WebSocket:** Altres clients connectats reben actualització automàtica
    - [ ] **Sincronització Desktop:** Aplicacions Tauri connectades reben el canvi via WebSocket
    - [ ] **CommandService:** Emissió d'event create amb connectionId per evitar loops

#### Cas de Fallada: Nom duplicat o sense permisos
- [ ] **Resum:** Intent de crear carpeta amb nom existent o en ubicació no permesa.
- [ ] **Validacions específiques del servidor:**
    - Verificació AccessType.WRITE al parent folder
    - Comprovació de noms duplicats a la mateixa carpeta
    - Validació de caràcters prohibits: `<>:"/\\|?*` i control chars
    - Noms reservats del sistema: CON, PRN, AUX, NUL, COM1-9, LPT1-9
- [ ] **Resultat Esperat:**
    - [ ] **Validació local:** Error mostrat abans d'enviar si el nom ja existeix
    - [ ] **API:** `403 Forbidden` si no hi ha permisos o `409 Conflict` si nom duplicat
    - [ ] **UI:**
        - Toast d'error amb missatge específic
        - El diàleg es manté obert per permetre correcció
        - [ ] **Rollback optimista:** Si es va mostrar optimista, es reverteix
    - [ ] **Base de dades:** No es produeix cap modificació

### Cas de Prova 9: Pujar Arxius (Upload)

#### Cas d'Èxit: Upload d'un arxiu individual
- [ ] **Resum:** L'usuari puja un arxiu mitjançant drag & drop o selecció.
- [ ] **Endpoint:** `POST /upload` (streaming) o `POST /files` (multipart)
- [ ] **Upload amb streaming (archivos grans):**
    1. Headers específics: parentId, fileName, elementId (opcional)
    2. X-Connection-Id per evitar loops de sincronització
    3. Stream processing amb HttpServletRequest.getInputStream()
    4. FileUtil.storeFile() guardat incremental
- [ ] **Procés de validació del servidor:**
    1. FileAccessService.checkAccessFolder() amb AccessType.WRITE
    2. Verificació de límits de mida (max-file-size: 100MB)
    3. Validació de tipus MIME i extensions
    4. Generació d'UUID per nom físic del fitxer
- [ ] **Procés a la UI:**
    1. L'usuari arrossega un arxiu a la zona del FileManager o fa clic "Upload File"
    2. Es selecciona l'arxiu mitjançant `<input type="file">`
    3. Es valida la mida i tipus d'arxiu
    4. Es mostra progress de pujada
- [ ] **Resultat Esperat:**
    - [ ] **API:** FileEntity amb metadades completes i FileInfo de resposta
    - [ ] **Almacenament físic:** 
        - Fitxer guardat a storage_data/{uuid}
        - Hash SHA calculat per verificació d'integritat
        - Metadades: nom original, mida, MIME type, timestamps
    - [ ] **UI:**
        - [ ] **Feedback visual:** Progress bar o spinner durant la pujada
        - [ ] **Actualització optimista:** L'arxiu apareix immediatament amb ID temporal
        - [ ] **TanStack Query:** Actualització de cache i refrescament
        - Toast d'èxit "File uploaded successfully"
    - [ ] **Base de dades:**
        - element_entity (isFolder: false)
        - file_entity: name, size, mimeType, userId, parentId, creationDate
        - access_rule: propietari amb ADMIN access
    - [ ] **Sincronització Desktop:** Aplicacions Tauri descarreguen automàticament el fitxer
    - [ ] **CommandService:** sendCreate() amb hash per verificació

#### Cas d'Èxit: Upload de carpeta completa
- [ ] **Resum:** L'usuari puja una carpeta sencera amb subcarpetes i arxius.
- [ ] **Procés de creació recursiva:**
    1. FileService.createFileFromUploadedTree() processa estructura
    2. Es creen carpetes en ordre jeràrquic (parents abans que children)
    3. Es pugen arxius amb referències correctes a parents
    4. Validació de permisos a cada nivell de la jerarquia
- [ ] **Procés a la UI:**
    1. L'usuari fa clic "Upload Folder" que activa `webkitdirectory`
    2. Selecciona una carpeta del sistema
    3. Es crea l'estructura recursiva abans de pujar
    4. Es pugen carpetes i arxius en l'ordre correcte
- [ ] **Resultat Esperat:**
    - [ ] **API:** Estructura completa creada amb referències correctes
    - [ ] **UI:**
        - [ ] **Actualització optimista:** Tota l'estructura apareix immediatament
        - Progress del procés de pujada múltiple
        - Toast d'èxit "Folder uploaded successfully"
    - [ ] **Base de dades:** Estructura completa creada recursivament
    - [ ] **Sincronització Desktop:** Estructura completa es replica a aplicacions Tauri

### Cas de Prova 10: Renomenar Element

#### Cas d'Èxit: Rename amb permisos adequats
- [ ] **Resum:** L'usuari renomena un arxiu o carpeta mitjançant el menú contextual.
- [ ] **Endpoint:** `PUT /files/{elementId}`
- [ ] **Procés de validació del servidor:**
    1. ElementService.elementNotDeleted() verifica que no estigui eliminat
    2. FileAccessService.checkAccessElement() amb AccessType.WRITE
    3. Validació de nom: caràcters prohibits i duplicats
    4. Actualització segons tipus (FolderService o FileService)
- [ ] **Operacions específiques:**
    - [ ] **Carpetes:** FolderService.updateFolderMetadata()
    - [ ] **Arxius:** FileService.updateFile() (només metadades, no contingut físic)
- [ ] **Procés a la UI:**
    1. Click dret sobre l'element → "Rename" o selecciona element i Shortcut
    2. S'obre el diàleg `RenameDialog`
    3. Es mostra el nom actual per editar
    4. Validació en temps real del nou nom
- [ ] **Resultat Esperat:**
    - [ ] **API:** Element actualitzat amb nou nom i lastModification timestamp
    - [ ] **Base de dades:**
        - folder_entity.name o file_entity.name actualitzat
        - lastModification timestamp actualitzat
    - [ ] **UI:**
        - [ ] **Actualització optimista:** El nom canvia immediatament
        - [ ] **Sincronització:** WebSocket notifica altres clients
        - [ ] **Cache:** TanStack Query actualitza totes les referències
        - Toast d'èxit "Item renamed successfully"
    - [ ] **Sincronització Desktop:** Aplicacions Tauri renombren l'element local automàticament
    - [ ] **CommandService:** sendUpdate() amb nou nom per sincronització

#### Cas de Fallada: Conflicte de nom
- [ ] **Resum:** Intent de rename a nom ja existent a la mateixa carpeta.
- [ ] **Validació del servidor:**
    - Cerca de noms duplicats dins del mateix parent
    - Verificació case-insensitive per compatibilitat de sistemes
- [ ] **Resultat Esperat:**
    - [ ] **Validació local:** Error mostrat abans d'enviar si detecta duplicat
    - [ ] **API:** `409 Conflict` "Name already exists"
    - [ ] **UI:** 
        - [ ] **Rollback optimista:** Reverteix el canvi visual
        - Toast d'error "Name already exists"
        - El diàleg es manté obert per correcció

### Cas de Prova 11: Moure Elements (Drag & Drop i Dialog)

#### Cas d'Èxit: Moviment amb Drag & Drop
- [ ] **Resum:** L'usuari arrossega elements a una carpeta de destinació.
- [ ] **Endpoint:** `PUT /files/{elementId}` amb nou parentId
- [ ] **Validacions específiques del servidor:**
    1. FileAccessService verifica WRITE access a origen i destinació
    2. Verificació de dependències circulars per carpetes
    3. Validació que destinació sigui una carpeta vàlida
    4. Comprovació de noms duplicats a destinació
- [ ] **Procés a la UI:**
    1. L'usuari fa clic i arrossega un element sobre una carpeta
    2. **DndContext** detecta l'operació de drop
    3. Es valida que la destinació sigui vàlida (no circular)
    4. Si hi ha múltiples elements seleccionats, es mouen tots
- [ ] **Resultat Esperat:**
    - [ ] **API:** Elements actualitzats amb nou parentId
    - [ ] **Validació anti-circular:**
        ```java
        throw new CircularDependencyException("Cannot move folder into itself or its children");
        ```
    - [ ] **UI:**
        - [ ] **Feedback visual:** L'element s'arrossega amb preview
        - [ ] **Actualització optimista:** Elements desapareixen d'origen i apareixen a destinació
        - [ ] **Validació:** No permet moure carpeta dins de si mateixa
        - Toast d'èxit "Items moved successfully"
    - [ ] **Base de dades:** Camp `parent_id` actualitzat a file_entity/folder_entity
    - [ ] **Sincronització Desktop:** Elements es mouen automàticament a les aplicacions Tauri
    - [ ] **CommandService:** sendUpdate() per cada element mogut

#### Cas d'Èxit: Moviment amb Dialog
- [ ] **Resum:** L'usuari usa "Move" del menú contextual.
- [ ] **Procés a la UI:**
    1. Click dret → "Move" obre `FolderSelectDialog`
    2. Es mostra l'arbre de carpetes disponibles
    3. L'usuari selecciona la destinació
    4. Es confirma el moviment
- [ ] **Resultat Esperat:** Idèntic al drag & drop però amb selecció manual

#### Cas de Fallada: Moviment circular o sense permisos
- [ ] **Resum:** Intent de moure carpeta dins de si mateixa o sense permisos.
- [ ] **Validacions del servidor:**
    - CircularDependencyException per moviments circulars
    - ForbiddenException per manca de permisos WRITE
- [ ] **Resultat Esperat:**
    - [ ] **Validació UI:** Es prevé l'operació i es mostra error
    - [ ] **API:** `400 Bad Request` (circular) o `403 Forbidden` (permisos)
    - [ ] **UI:** Toast d'error amb raó específica

### Cas de Prova 12: Copiar Elements

#### Cas d'Èxit: Copy amb permisos adequats
- [ ] **Resum:** L'usuari copia elements mitjançant Ctrl+C i Ctrl+V.
- [ ] **Endpoint:** `POST /files/{elementId}/copy/{newParentId}`
- [ ] **Procés de còpia del servidor:**
    1. Verificació de permisos READ a origen i WRITE a destinació
    2. **Arxius:** FileUtil.copyFile() duplica contingut físic amb nou UUID
    3. **Carpetes:** Còpia recursiva de tota l'estructura
    4. Resolució automàtica de conflictes de noms (afegint sufixos)
    5. Creació de noves AccessRules pel copiat
- [ ] **Procés a la UI:**
    1. L'usuari selecciona elements i prem Ctrl+C (o Cmd+C)
    2. El `useFileShortcuts` hook gestiona l'operació
    3. Elements es guarden al clipboard del store
    4. L'usuari navega a destinació i prem Ctrl+V
    5. Es criden les operacions de còpia per cada element
- [ ] **Resultat Esperat:**
    - [ ] **API:** Noves entitats creades com a duplicats independents
    - [ ] **Almacenament físic:** 
        - Arxius: nou fitxer físic amb UUID diferent
        - Hash independent per verificació d'integritat
    - [ ] **Resolució de conflictes automàtica:**
        - `document.txt` → `document (1).txt`
        - `folder` → `folder (1)`
    - [ ] **UI:**
        - [ ] **Clipboard visual:** Elements copiats es mantenen visibles sense canvis
        - [ ] **Actualització optimista:** Còpies apareixen a destinació
        - [ ] **Resolució de conflictes:** Si existeix nom, s'afegeix sufijo "(1)"
        - Toast d'èxit "Items copied successfully"
    - [ ] **Base de dades:** Noves entrades creades com a rèpliques independents
    - [ ] **Sincronització Desktop:** Còpies apareixen a les aplicacions Tauri

### Cas de Prova 13: Descarregar Elements

#### Cas d'Èxit: Download d'arxiu individual
- [ ] **Resum:** L'usuari descarrega un arxiu mitjançant doble clic o menú contextual.
- [ ] **Endpoint:** `GET /files/{elementId}/download`
- [ ] **Procés de descàrrega del servidor:**
    1. FileAccessService.checkAccessElement() amb AccessType.READ
    2. FileUtil.loadAsResource() carrega fitxer físic
    3. Headers de resposta amb Content-Disposition i Content-Length
    4. Streaming de contingut per arxius grans
- [ ] **Procés a la UI:**
    1. Doble clic sobre arxiu o click dret → "Download"
    2. Es verifica que l'usuari té permisos de lectura
    3. Es fa petició de download
- [ ] **Resultat Esperat:**
    - [ ] **API:** Stream de bytes amb headers apropiats:
        ```
        Content-Type: application/octet-stream
        Content-Disposition: attachment; filename="document.txt"
        Content-Length: 1024
        ```
    - [ ] **UI:** El navegador inicia la descàrrega automàticament
    - [ ] **Base de dades:** Operació de només lectura

#### Cas d'Èxit: Download de carpeta (ZIP)
- [ ] **Resum:** L'usuari descarrega una carpeta sencera.
- [ ] **Procés del servidor:**
    1. FolderService.downloadFolder() crea ZIP temporal
    2. Compressió recursiva de tota l'estructura
    3. Stream del ZIP generat dinàmicament
    4. Cleanup automàtic del fitxer temporal
- [ ] **Resultat Esperat:**
    - [ ] **API:** ZIP stream amb nom `{folderName}.zip`
    - [ ] **UI:** Es descarrega un fitxer ZIP amb tot el contingut
    - [ ] **Servidor:** El servidor genera el ZIP dinàmicament sense persistència

#### Cas d'Èxit: Download múltiple (Bulk)
- [ ] **Resum:** L'usuari descarrega múltiples elements seleccionats.
- [ ] **Endpoint:** `POST /files/download/bulk`
- [ ] **Procés del servidor:**
    1. FileService.downloadMultipleElementsAsZip() processa llista d'IDs
    2. Verificació de permisos per cada element
    3. Compressió de tots els elements en un ZIP
- [ ] **Resultat Esperat:**
    - [ ] **API:** ZIP amb tots els elements seleccionats
    - [ ] **UI:** Download automàtic de "download.zip"

### Cas de Prova 13B: Endpoints Específics d'Estructura i Gestió Interna

#### Cas d'Èxit: Obtenir estructura completa de carpetes
- [ ] **Resum:** L'aplicació obté l'estructura jeràrquica completa per construir l'arbre de navegació.
- [ ] **Endpoint:** `GET /files/structure`
- [ ] **Procés del servidor:**
    1. FolderService.getFolderStructure() construeix arbre complet
    2. Retorna estructura amb tres seccions: root, shared, trash
    3. Inclou només carpetes, no arxius individuals
- [ ] **Resultat Esperat:**
    - [ ] **API:** FolderStructure amb jerarquia completa de carpetes
    - [ ] **UI:** Sidebar es carrega amb estructura expandible
    - [ ] **Cache:** TanStack Query emmagatzema estructura per navegació ràpida

#### Cas d'Èxit: Obtenir contingut complet de carpeta amb detalls
- [ ] **Resum:** Carregar carpeta amb tots els detalls per a visualització completa.
- [ ] **Endpoint:** `GET /files/{folderId}/full?deleted={boolean}`
- [ ] **Procés del servidor:**
    1. FolderService.getFolderByElementId() carrega carpeta
    2. FileAccessService verifica permisos READ
    3. FolderService.setDetails() afegeix metadades additionals
    4. Suport per carpetes eliminades amb paràmetre deleted=true
- [ ] **Resultat Esperat:**
    - [ ] **API:** FolderInfo amb arxius, subcarpetes i metadades completes
    - [ ] **Metadades incloses:** 
        - Permisos d'accés de l'usuari actual
        - Timestamps de creació i modificació
        - Indicadors de propietat i compartició
        - Mida total i nombre d'elements
    - [ ] **UI:** Vista completa amb tots els detalls necessaris per la interfície

#### Cas d'Èxit: Endpoints interns per comunicació entre microserveis
- [ ] **Resum:** Endpoints utilitzats per comunicació interna entre microserveis.
- [ ] **Endpoints interns específics:**
    - `GET /files/internal/folder/{elementId}` - Obtenir carpeta per altres serveis
    - `GET /files/internal/children/{elementId}` - Obtenir llista d'IDs de fills
    - `GET /files/internal/{elementId}/parent` - Obtenir ID del parent
    - `GET /files/internal/{elementId}/parent/element` - Obtenir entitat parent completa
    - `GET /files/internal/elements/structure?ids={elementIds}` - Estructura per IDs específics
- [ ] **Procés de validació:**
    1. Aquests endpoints no requereixen autenticació d'usuari (intern)
    2. Utilitzats per FileSharing, Trash, SyncService per obtenir metadades
    3. Retornen només informació necessària per operacions internes
- [ ] **Resultat Esperat:**
    - [ ] **API:** Resposta optimitzada per comunicació interna
    - [ ] **No inclou:** Verificacions de permisos d'usuari final
    - [ ] **Performance:** Respostes ràpides per operacions entre serveis

#### Cas d'Èxit: Operacions de canvi d'estat per a paperera
- [ ] **Resum:** Gestió interna de l'estat d'eliminació d'elements.
- [ ] **Endpoints d'estat:**
    - `PUT /files/internal/elements/{elementId}/state` - Canviar estat eliminat
    - `DELETE /files/internal/elements/{elementId}/permanent` - Eliminació permanent
- [ ] **Procés de canvi d'estat:**
    1. ElementService.setDeletedState() processa recursivament
    2. CommandService envia events WebSocket per cada element afectat
    3. FileUtil.deleteFile() elimina contingut físic en eliminació permanent
- [ ] **Resultat Esperat:**
    - [ ] **API:** SetDeletedResponse amb llista d'IDs afectats
    - [ ] **Base de dades:** Camp deleted actualitzat recursivament
    - [ ] **Sincronització:** Events WebSocket per cada canvi d'estat
    - [ ] **Cleanup:** Eliminació física de fitxers en eliminació permanent

---

## UC-11, UC-12, UC-17: Gestió de la Paperera a la Interfície Web

### Cas de Prova 14: Enviar a la Paperera

#### Cas d'Èxit: Eliminació suau des de "Els meus arxius"
- [ ] **Resum:** L'usuari elimina elements que van a la paperera.
- [ ] **Endpoint:** `DELETE /files/{elementId}` (redirigit a `PUT /trash/move/{elementId}`)
- [ ] **Procés a la UI:**
    1. L'usuari selecciona elements i prem Delete o click dret → "Delete"
    2. Es confirma l'operació (opcional)
    3. Els elements es mouen a la secció Trash
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - [ ] **Actualització optimista:** Elements desapareixen de la vista actual
        - [ ] **Secció Trash:** Elements apareixen a la paperera amb data d'eliminació
        - [ ] **Visual feedback:** Elements amb appearance diferent (grisats)
        - Toast d'èxit "Items moved to trash correctly"
        - [ ] **WebSocket:** Sincronització automàtica amb altres clients
    - [ ] **Base de dades:**
        - Camp `deleted` marcat com `true` a `file_entity`/`folder_entity`
        - Nou registre a `trash_record` amb data d'expiració
    - [ ] **Sincronització Desktop:** Elements eliminats també a les aplicacions Tauri

### Cas de Prova 15: Restaurar des de la Paperera

#### Cas d'Èxit: Restore per part del propietari
- [ ] **Resum:** L'usuari restaura elements des de la vista Trash.
- [ ] **Endpoint:** `PUT /trash/{elementId}/restore`
- [ ] **Procés a la UI:**
    1. L'usuari navega a la secció "Trash"
    2. Selecciona elements i click dret → "Restore" 
    3. Elements es mouen de tornada a la seva ubicació original
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - [ ] **Actualització optimista:** Elements desapareixen de Trash
        - [ ] **Secció origen:** Elements reapareixen a la seva ubicació original
        - Toast d'èxit "Items restored correctly"
        - [ ] **Navegació automàtica:** Si és possible, navega a la ubicació restaurada
    - [ ] **Base de dades:**
        - Camp `deleted` marcat com `false`
        - Registre `trash_record` eliminat
    - [ ] **Sincronització Desktop:** Elements restaurats també a les aplicacions Tauri

### Cas de Prova 16: Eliminació permanent

#### Cas d'Èxit: Delete permanent des de Trash
- [ ] **Resum:** L'usuari elimina permanentment des de la paperera.
- [ ] **Endpoint:** `DELETE /trash/{elementId}`
- [ ] **Procés a la UI:**
    1. Des de la secció Trash, l'usuari selecciona elements
    2. Click dret → "Delete permanently" o botó específic
    3. Confirmació addicional per l'operació irreversible
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - [ ] **Diàleg de confirmació:** "This action cannot be undone"
        - [ ] **Actualització optimista:** Elements desapareixen definitivament
        - Toast d'èxit "Items deleted permanently"
    - [ ] **Base de dades:** Eliminació física de totes les entrades relacionades
    - [ ] **Sincronització Desktop:** Elements eliminats permanentment de les aplicacions Tauri

---

## UC-13: Compartició d'Arxius a la Interfície Web

### Cas de Prova 17: Compartir Elements

#### Cas d'Èxit: Share amb usuaris existents
- [ ] **Resum:** L'usuari comparteix elements amb altres usuaris.
- [ ] **Endpoints:** `POST /share`, `GET /users/search`
- [ ] **Procés a la UI:**
    1. L'usuari selecciona elements i click dret → "Share"
    2. S'obre el `ShareDialog` amb els elements seleccionats
    3. L'usuari cerca usuaris amb `GET /users/search`
    4. Selecciona usuaris i assigna permisos (READ/WRITE)
    5. Confirma la compartició
- [ ] **Resultat Esperat:**
    - [ ] **Cerca d'usuaris:** 
        - Input amb autocomplete
        - Llista d'usuaris filtrada en temps real
        - Validació que l'usuari existeix
    - [ ] **Gestió de permisos:**
        - [ ] **Visual:** Checkboxes per cada combinació usuari-arxiu
        - [ ] **Dropdown:** Selecció de READ/WRITE per cada element
        - [ ] **Preview:** Llista dels permisos abans de confirmar
    - [ ] **API:** Múltiples peticions de compartició
    - [ ] **UI:**
        - Toast d'èxit "Items shared successfully"
        - [ ] **Actualització:** Elements marcats com a compartits
        - [ ] **Indicador visual:** Icona de compartit sobre els elements
    - [ ] **Base de dades:**
        - Noves entrades a `shared_access` per cada usuari-element
        - Noves regles a `access_rule` amb permisos especificats
    - [ ] **Sincronització:** 
        - [ ] **WebSocket:** Usuaris receptors reben notificació
        - [ ] **Secció Shared:** Elements apareixen a "Compartit amb mi" dels receptors
        - [ ] **Desktop:** Aplicacions Tauri dels usuaris receptors reben els elements

#### Cas de Fallada: Usuari inexistent
- [ ] **Resum:** Intent de compartir amb usuari que no existeix.
- [ ] **Resultat Esperat:**
    - [ ] **Cerca UI:** No mostra resultats per l'usuari inexistent
    - [ ] **API (si es força):** `404 Not Found`
    - [ ] **UI:** Toast d'error "User not found"

### Cas de Prova 18: Gestionar Permisos Existents

#### Cas d'Èxit: Modificar permisos de compartició
- [ ] **Resum:** L'usuari modifica permisos d'elements ja compartits.
- [ ] **Endpoints:** `GET /share/user/{elementId}`, `PUT /share`
- [ ] **Procés a la UI:**
    1. Obre ShareDialog per element ja compartit
    2. Es carreguen els permisos actuals amb `GET /share/user/{elementId}`
    3. L'usuari modifica permisos o afegeix/elimina usuaris
    4. Es calculen les diferències per enviar només canvis necessaris
- [ ] **Resultat Esperat:**
    - [ ] **Càrrega d'estat:** Dialog mostra permisos actuals
    - [ ] **Operacions optimitzades:** Només s'envien canvis reals
    - [ ] **API:** Combinació de POST/PUT/DELETE segons canvis
    - [ ] **UI:** Toast d'èxit amb resum de canvis aplicats

### Cas de Prova 19: Revocar Accés

#### Cas d'Èxit: Owner revoca accés
- [ ] **Resum:** El propietari elimina l'accés d'un usuari.
- [ ] **Endpoint:** `DELETE /share/{elementId}/user/{username}`
- [ ] **Procés a la UI:**
    1. Des del ShareDialog, l'usuari elimina un usuari de la llista
    2. O des de la secció Shared, click dret → "Revoke access"
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - [ ] **Actualització optimista:** Usuari desapareix de la llista
        - Toast d'èxit "Access revoked correctly"
    - [ ] **Base de dades:** Eliminació d'entrades `shared_access` i `access_rule`
    - [ ] **Sincronització:** 
        - [ ] **WebSocket:** Usuari receptor és notificat
        - [ ] **UI receptor:** Element desapareix de "Compartit amb mi"
        - [ ] **Desktop:** Element eliminat de les aplicacions Tauri del receptor

#### Cas d'Èxit: Usuari receptor deixa de seguir
- [ ] **Resum:** Un usuari receptor elimina element de "Compartit amb mi".
- [ ] **Procés a la UI:**
    1. Des de la secció "Shared", l'usuari fa Delete o click dret → "Stop following"
    2. Confirmació de l'acció
- [ ] **Resultat Esperat:** Mateix efecte que revocació per part del propietari

---

## UC-05 a UC-07 i UC-18 a UC-21: Administració a la Interfície Web

### Cas de Prova 20: Panel d'Administració

#### Cas d'Èxit: Accés com a administrador
- [ ] **Resum:** Un usuari amb rol ADMIN/SUPER_ADMIN accedeix al panel.
- [ ] **Endpoint:** `GET /admin/users`
- [ ] **Procés de validació del servidor:**
    1. AdminController verifica rol de l'usuari sol·licitant
    2. AdminService.getAllUsers() consulta UserRepository i UserAuthClient
    3. Es combinen dades de user_info (UserManagement) i user_entity (UserAuthentication)
    4. Es filtra informació segons si l'usuari és SUPER_ADMIN
- [ ] **Procés a la UI:**
    1. L'usuari fa clic a "Admin Dashboard" al header (només visible per admins)
    2. Es carrega el component `AdminDashboard`
    3. Es fa petició per llistar tots els usuaris
- [ ] **Resultat Esperat:**
    - [ ] **API:** Retorna llista UserDetails[] amb informació combinada
    - [ ] **Dades retornades per cada usuari:**
        - id, username, email, firstName, lastName (de user_info)
        - role, lastPasswordChange (de user_entity)
        - createdDate, lastModifiedDate (de user_info)
        - password visible només per SUPER_ADMIN
    - [ ] **UI:**
        - [ ] **Taula d'usuaris:** Mostra username, email, rol, data creació, últim login
        - [ ] **Filtres i cerca:** Input per filtrar usuaris en temps real
        - [ ] **Accions per fila:** Botons Edit i Delete segons permisos
        - [ ] **Paginació:** Si hi ha molts usuaris
        - [ ] **Estats de càrrega:** Skeleton loaders durant càrrega inicial
    - [ ] **Base de dades:** Consulta combinada user_info + user_entity via Feign calls

#### Cas de Fallada: Accés no autoritzat
- [ ] **Resum:** Usuari amb rol USER intenta accedir al panel.
- [ ] **Resultat Esperat:**
    - [ ] **UI:** Link d'admin no apareix al header
    - [ ] **Gateway:** `403 Forbidden` si s'intenta accedir directament
    - [ ] **UI (si accedeix a URL):** Redirecció o error 403

### Cas de Prova 21: Editar Usuari com a Administrador

#### Cas d'Èxit: SUPER_ADMIN modifica usuari
- [ ] **Resum:** Un SUPER_ADMIN edita dades d'un altre usuari.
- [ ] **Endpoints:** `GET /admin/users/{username}`, `PUT /admin/users/{userId}`
- [ ] **Jerarquía de permisos específica:**
    - [ ] **SUPER_ADMIN:** Pot modificar qualsevol usuari (inclòs altres SUPER_ADMIN)
    - [ ] **ADMIN:** Pot modificar només usuaris amb rol USER
    - [ ] **Protecció especial:** SUPER_ADMIN no pot rebaixar el seu propi rol
- [ ] **Procés de validació del servidor:**
    1. AdminController verifica permisos segons jerarquía
    2. AdminService.updateUser() actualitza user_info i crida UserAuthClient
    3. UserAuthentication actualitza user_entity amb les noves dades
    4. Si inclou password, es valida amb patterns específics
- [ ] **Procés a la UI:**
    1. L'admin fa clic "Edit" a la taula d'usuaris
    2. S'obre `EditUserDialog` amb dades actuals
    3. Es permeten canvis a username, email, firstName, lastName, role
    4. Validació en temps real amb disponibilitat
    5. Confirmació dels canvis
- [ ] **Resultat Esperat:**
    - [ ] **Validacions específiques:**
        - [ ] **Temps real:** Check de disponibilitat d'username/email
        - [ ] **Jerarquia de rols:** ADMIN no pot editar SUPER_ADMIN
        - [ ] **Auto-protecció:** SUPER_ADMIN no pot rebaixar-se el seu propi rol
        - [ ] **Password:** Si es proporciona, es valida i encripta amb BCrypt
    - [ ] **UI:**
        - [ ] **Dialog modal:** Formulari amb tots els camps editables
        - [ ] **Visual feedback:** Icones de validació per cada camp
        - [ ] **Confirmació:** Canvis es reflecteixen immediatament a la taula
        - Toast d'èxit "User updated successfully"
    - [ ] **Base de dades:**
        - user_info actualitzat via UserManagement
        - user_entity actualitzat via UserAuthentication
        - lastPasswordChange actualitzat si es canvia password

#### Cas de Fallada: Violació de jerarquia de rols
- [ ] **Resum:** ADMIN intenta modificar un SUPER_ADMIN.
- [ ] **Validacions del servidor:**
    ```java
    if(current.equals(Roles.SUPER_ADMIN) || 
       (toUpdate.equals(Roles.USER) && current.equals(Roles.ADMIN))) {
        // Permitit
    } else {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    ```
- [ ] **Resultat Esperat:**
    - [ ] **UI:** Botó "Edit" deshabilitat per SUPER_ADMIN si l'usuari és ADMIN
    - [ ] **API (si es força):** `403 Forbidden`
    - [ ] **UI:** Toast d'error sobre permisos insuficients

#### Cas de Fallada: SUPER_ADMIN intenta rebaixar-se el propi rol
- [ ] **Resum:** Un SUPER_ADMIN intenta canviar el seu propi rol a ADMIN o USER.
- [ ] **Validació del servidor:**
    ```java
    if(toUpdate.equals(Roles.SUPER_ADMIN) && 
       !userDTO.getRole().equals(Roles.SUPER_ADMIN.name())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    ```
- [ ] **Resultat Esperat:**
    - [ ] **API:** `403 Forbidden`
    - [ ] **UI:** Error message "Cannot demote your own SUPER_ADMIN role"

### Cas de Prova 22: Eliminar Usuari com a Administrador

#### Cas d'Èxit: SUPER_ADMIN elimina usuari
- [ ] **Resum:** Un SUPER_ADMIN elimina el compte d'un altre usuari.
- [ ] **Endpoint:** `DELETE /admin/users/{username}`
- [ ] **Jerarquía de permisos per eliminació:**
    - [ ] **SUPER_ADMIN:** Pot eliminar ADMIN i USER (però no altres SUPER_ADMIN)
    - [ ] **ADMIN:** Pot eliminar només USER
- [ ] **Procés d'eliminació idèntic al cas propi:**
    1. AdminController verifica permisos segons jerarquía
    2. UserDeletionService.startUserDeletion() inicia procés asíncron
    3. Mateix workflow d'orquestació RabbitMQ que eliminació pròpia
- [ ] **Procés a la UI:**
    1. L'admin fa clic "Delete" a la taula
    2. S'obre `DeleteUserDialog` amb confirmació
    3. L'admin confirma l'eliminació
- [ ] **Resultat Esperat:**
    - [ ] **Validació de jerarquía:**
        ```java
        if(!toDelete.equals(Roles.SUPER_ADMIN) && 
           (current.equals(Roles.SUPER_ADMIN) || 
            (current.equals(Roles.ADMIN) && toDelete.equals(Roles.USER)))) {
            // Permitit
        }
        ```
    - [ ] **UI:**
        - [ ] **Dialog de confirmació:** Advertència sobre irreversibilitat
        - [ ] **Actualització optimista:** Usuari desapareix de la taula
        - Toast d'èxit "User deleted successfully"
    - [ ] **Base de dades:** Idèntic procés asíncron que eliminació pròpia
    - [ ] **Efectes secundaris:** 
        - [ ] **Sessions:** Sessions actives de l'usuari es tanquen
        - [ ] **WebSocket:** L'usuari és desconnectat automàticament
        - [ ] **Desktop:** Si l'usuari té aplicacions Tauri obertes, es desconnecten

#### Cas de Fallada: Intent d'eliminar SUPER_ADMIN
- [ ] **Resum:** Qualsevol usuari (inclòs SUPER_ADMIN) intenta eliminar un SUPER_ADMIN.
- [ ] **Validació del servidor:**
    ```java
    if(!toDelete.equals(Roles.SUPER_ADMIN)) {
        // Només si NO és SUPER_ADMIN
    }
    ```
- [ ] **Resultat Esperat:**
    - [ ] **API:** `403 Forbidden`
    - [ ] **UI:** Botó "Delete" deshabilitat per usuaris SUPER_ADMIN

### Cas de Prova 22B: Verificació d'Endpoints Administratius Específics

#### Cas d'Èxit: Endpoints de cerca i verificació
- [ ] **Endpoints específics d'administració:**
    - `GET /users/search?q={query}` - Cerca usuaris per username/email
    - `GET /users/check-email?email={email}` - Verificar disponibilitat email  
    - `GET /users/check-username?username={username}` - Verificar disponibilitat username
    - `GET /users/{username}/id` - Obtenir UUID per username
    - `POST /users/auth/usernames` - Obtenir usernames per llista d'UUIDs
    - `GET /users/auth/internal/search-ids-by-username?q={query}` - Cerca IDs per username
- [ ] **Cerca d'usuaris combinada:**
    1. UserManagement cerca per email a user_info
    2. UserAuthentication cerca per username a user_entity  
    3. Es combinen resultats eliminant duplicats
    4. Es resol usernames via UserAuthClient.getUsernamesByIds()
- [ ] **Verificació de disponibilitat:**
    - Email: UserManagement.findByEmailIgnoreCase()
    - Username: UserAuthentication.findByUsernameIgnoreCase()
- [ ] **Resultat Esperat:**
    - [ ] **Cerca retorna:** UserSearchResult[] amb email i username
    - [ ] **Verificacions retornen:** Boolean (true = disponible)
    - [ ] **ID mapping:** Conversió bidireccional UUID ↔ username

---

## Casos de Prova de Funcionalitats Específiques de la UI

### Cas de Prova 23: Selecció Múltiple i Shortcuts

#### Cas d'Èxit: Operacions amb teclat
- [ ] **Resum:** L'usuari utilitza shortcuts per gestionar arxius.
- [ ] **Shortcuts disponibles:**
    - `Ctrl+C` / `Cmd+C`: Copiar elements seleccionats
    - `Ctrl+X` / `Cmd+X`: Tallar elements seleccionats
    - `Ctrl+V` / `Cmd+V`: Enganxar elements del clipboard
    - `Ctrl+A` / `Cmd+A`: Seleccionar tots els elements
    - `Delete`: Eliminar elements seleccionats
    - `Shift+↑/↓/←/→`: Selecció amb fletxes
- [ ] **Procés a la UI:**
    1. L'usuari selecciona elements amb Ctrl+Click (múltiple) o Shift+Click (rang)
    2. Utilitza shortcuts per operacions
    3. El `useFileShortcuts` hook gestiona tots els events
- [ ] **Resultat Esperat:**
    - [ ] **Selecció múltiple:**
        - [ ] **Visual:** Elements seleccionats amb highlight
        - [ ] **Contador:** "X items selected" a la interfície
        - [ ] **Operacions:** Totes les operacions apliquen a la selecció
    - [ ] **Clipboard:**
        - [ ] **Cut:** Elements es mostren semi-transparents
        - [ ] **Copy:** Elements es mantenen normals
        - [ ] **Paste:** Elements s'afegeixen a la destinació
    - [ ] **Validació:** Shortcuts només funcionen si hi ha permisos adequats

### Cas de Prova 24: Navegació i Sidebar

#### Cas d'Èxit: Navegació entre seccions
- [ ] **Resum:** L'usuari navega entre "Els meus arxius", "Compartit amb mi" i "Paperera".
- [ ] **Componens:** `FileDirectorySidebar`, `TabsSubHeader`
- [ ] **Procés a la UI:**
    1. L'usuari fa clic a les seccions del sidebar o tabs
    2. El `useFileContextStore` actualitza la secció activa
    3. Es carreguen les dades corresponents
- [ ] **Resultat Esperat:**
    - [ ] **Persistència:** La secció es manté en sessionStorage
    - [ ] **Breadcrumbs:** Actualització del path actual
    - [ ] **Contingut:** Càrrega de dades específiques per secció
    - [ ] **Permisos:** Operacions disponibles segons la secció

#### Cas d'Èxit: Expansió de carpetes al sidebar
- [ ] **Resum:** L'usuari expandeix/col·lapsa carpetes al sidebar.
- [ ] **Procés a la UI:**
    1. Click a l'icona d'arrow per expandir/col·lapsar
    2. Es carreguen subcarpetes si cal
    3. L'estat es persisteix en sessionStorage
- [ ] **Resultat Esperat:**
    - [ ] **Visual:** Animació d'expansió/col·lapse
    - [ ] **Persistència:** Estat mantingut entre sessions
    - [ ] **Performance:** Lazy loading de subcarpetes

### Cas de Prova 25: Drag & Drop Avançat

#### Cas d'Èxit: Múltiples elements amb drag & drop
- [ ] **Resum:** L'usuari arrossega múltiples elements alhora.
- [ ] **Procés a la UI:**
    1. L'usuari selecciona múltiples elements
    2. Arrossega un dels elements seleccionats
    3. Tots els elements seleccionats es mouen junts
- [ ] **Resultat Esperat:**
    - [ ] **Visual:** Preview mostra tots els elements
    - [ ] **Validació:** Comprova permisos per tots els elements
    - [ ] **Operació:** Moviment atòmic de tots els elements

### Cas de Prova 26: Filtres i Ordenació

#### Cas d'Èxit: Aplicació de filtres
- [ ] **Resum:** L'usuari filtra per tipus d'element i ordena per criteris.
- [ ] **Opcions disponibles:**
    - [ ] **Filtres:** Mostrar només arxius, només carpetes, o tots
    - [ ] **Ordenació:** Nom (A-Z, Z-A), Data creació, Data modificació, Mida, Tipus
- [ ] **Procés a la UI:**
    1. L'usuari utilitza els controls de filtre/ordenació
    2. El `useFileContextStore` actualitza els criteris
    3. La funció `filterAndSort` aplica els canvis
- [ ] **Resultat Esperat:**
    - [ ] **Actualització:** Vista es reordena/filtra immediatament
    - [ ] **Persistència:** Criteris es mantenen en localStorage
    - [ ] **Performance:** Filtratge i ordenació optimitzats

---

## Casos de Prova de Sincronització WebSocket

### Cas de Prova 27: Sincronització en Temps Real

#### Cas d'Èxit: Actualització automàtica per canvis externs
- [ ] **Resum:** Un usuari rep actualitzacions quan un altre usuari modifica arxius compartits.
- [ ] **Endpoint WebSocket:** `/websocket/web` (UI) i `/websocket` (Desktop)
- [ ] **Procés:**
    1. Usuari A modifica un arxiu compartit amb Usuari B
    2. El servidor envia missatge WebSocket a Usuari B
    3. Les UIs de Usuari B s'actualitzen automàticament
- [ ] **Missatges WebSocket:**
    ```json
    {
      "type": "COMMAND",
      "message": "updated_tree",
      "data": {
        "parentId": "folder-id",
        "elementId": "element-id",
        "section": "shared"
      },
      "timestamp": 1234567890
    }
    ```
- [ ] **Resultat Esperat:**
    - [ ] **Web UI:** `websocketService.update()` processa el missatge
    - [ ] **Desktop:** `handle_msg()` processa i aplica canvis al sistema de fitxers
    - [ ] **Actualització:** 
        - Si l'usuari està veient la carpeta afectada: `refreshAll()`
        - Si està en la secció afectada: `refreshAll()`
        - Altrament: `refreshFolderStructure()`
    - [ ] **UI:** Canvis apareixen sense intervenció de l'usuari
    - [ ] **Desktop:** Fitxers locals actualitzats automàticament

#### Cas d'Èxit: Sincronització bidireccional Web ↔ Desktop
- [ ] **Resum:** Canvis fets a la web apareixen al desktop i viceversa.
- [ ] **Procés:**
    1. **Web → Desktop:** Usuari crea fitxer a la web UI
        - WebSocket notifica aplicació Tauri
        - Tauri descarrega el fitxer automàticament
        - Fitxer apareix al sistema de fitxers local
    2. **Desktop → Web:** Usuari afegeix fitxer a la carpeta local
        - File watcher detecta el canvi
        - Tauri puja el fitxer al servidor
        - WebSocket notifica web UI
        - Fitxer apareix a la web UI
- [ ] **Resultat Esperat:**
    - [ ] **Consistència:** Ambdues interfícies mostren el mateix estat
    - [ ] **Temps real:** Canvis apareixen immediatament
    - [ ] **Bidireccional:** Funciona en ambdues direccions

#### Cas d'Èxit: Reconnexió automàtica
- [ ] **Resum:** La connexió WebSocket es restableix automàticament després d'una desconnexió.
- [ ] **Procés:**
    1. La connexió WebSocket es perd (problemes de xarxa, servidor reiniciat)
    2. Els serveis WebSocket detecten la desconnexió
    3. Intenten reconnectar amb backoff exponential
- [ ] **Resultat Esperat:**
    - [ ] **Web UI:** 
        - Retry logic fins a 10 intents amb delays crescents (1s, 2s, 4s, ..., màx 30s)
        - UI feedback opcional indicador de connexió
    - [ ] **Desktop:**
        - Loop infinit amb sleep de 5s entre intents
        - `IS_CONNECTED = false` i event `is_connected: false` emès
        - UI mostra overlay "Connection Lost"
    - [ ] **Sincronització:** Un cop reconnectat, els usuaris reben actualitzacions pendents

#### Cas de Fallada: Token WebSocket invàlid
- [ ] **Resum:** Intent de connexió WebSocket amb token expirat.
- [ ] **Resultat Esperat:**
    - [ ] **Web UI:** 
        - Connexió falla i no es reintenta
        - Usuari redirigit a login si el token és completament invàlid
    - [ ] **Desktop:**
        - Connexió falla, es marca com desconnectat
        - Token watcher detecta expiració i inicia logout automàtic

---

## Casos de Prova d'Operacions Desktop Específiques

### Cas de Prova 28: Gestió d'Operacions de Fitxers Desktop

#### Cas d'Èxit: Creació de carpeta des del sistema de fitxers
- [ ] **Resum:** L'usuari crea una carpeta directament al sistema de fitxers local.
- [ ] **Procés a l'Aplicació Desktop:**
    1. File watcher detecta event `EventKind::Create(CreateKind::Folder)`
    2. Es construeix node local amb `fstree::build_node()`
    3. S'afegeix al tree local amb `add_node()`
    4. Es crida `api::create_folder()` per sincronitzar amb servidor
- [ ] **Endpoint utilitzat:** `POST /files`
- [ ] **Resultat Esperat:**
    - [ ] **Sistema local:** Carpeta ja existeix (creada per l'usuari)
    - [ ] **API:** Carpeta creada al servidor amb metadades
    - [ ] **Base de dades:**
        - Nova entrada a `element_entity` (tipus `FOLDER`)
        - Nova entrada a `folder_entity` amb el `parent_id` correcte
        - Nova regla de propietat a `access_rule`
    - [ ] **Sincronització:** Altres clients reben la carpeta via WebSocket
    - [ ] **Response:** ID del servidor s'assigna al node local

#### Cas d'Èxit: Upload automàtic de fitxer afegit localment
- [ ] **Resum:** L'usuari afegeix un fitxer a la carpeta local sincronitzada.
- [ ] **Procés a l'Aplicació Desktop:**
    1. File watcher detecta `EventKind::Create(CreateKind::File)`
    2. Es crea `Transfer` amb tipus `Upload` i estat `Active`
    3. Es construeix multipart form amb fitxer i metadades
    4. `api::upload()` envia el fitxer al servidor amb progress tracking
- [ ] **Endpoint utilitzat:** `POST /upload`
- [ ] **Resultat Esperat:**
    - [ ] **Progress tracking:** Event `transfer` emès amb progrés 0-100%
    - [ ] **API:** Fitxer pujat amb success
    - [ ] **Base de dades:**
        - Nova entrada a `element_entity` (tipus `FILE`)
        - Nova entrada a `file_entity` amb metadades (nom, mida, MIME type)
        - Nova regla de propietat a `access_rule`
    - [ ] **UI Desktop:** Transfer apareix a tab "Current transfers", després a "Completed"
    - [ ] **Sincronització:** Altres clients reben el fitxer via WebSocket

#### Cas d'Èxit: Eliminació de fitxer des del sistema local
- [ ] **Resum:** L'usuari elimina un fitxer del sistema de fitxers local.
- [ ] **Procés a l'Aplicació Desktop:**
    1. File watcher detecta `EventKind::Remove(RemoveKind::File)`
    2. Es localitza el node a l'arbre local
    3. S'elimina del tree local amb `delete_node()`
    4. Es crida `api::delete()` per eliminar del servidor
- [ ] **Endpoint utilitzat:** `DELETE /files/{elementId}`
- [ ] **Resultat Esperat:**
    - [ ] **Sistema local:** Fitxer ja eliminat (per l'usuari)
    - [ ] **API:** Element enviat a paperera al servidor
    - [ ] **Base de dades:**
        - Camp `deleted` marcat com `true` a `file_entity`
        - Nou registre a `trash_record`
    - [ ] **Sincronització:** Altres clients veuen l'element a la paperera

#### Cas d'Èxit: Renombrar element des del sistema local
- [ ] **Resum:** L'usuari renombra un fitxer/carpeta al sistema de fitxers.
- [ ] **Procés a l'Aplicació Desktop:**
    1. File watcher detecta `EventKind::Modify(ModifyKind::Name(RenameMode::Both))`
    2. Sistema detecta rename amb `detect_renames()`
    3. S'actualitza l'arbre local amb `rename_node()`
    4. Es crida `api::rename()` per actualitzar al servidor
- [ ] **Endpoint utilitzat:** `PUT /files/{elementId}`
- [ ] **Resultat Esperat:**
    - [ ] **Sistema local:** Element ja renombrat
    - [ ] **API:** Nom actualitzat al servidor
    - [ ] **Base de dades:** Camp `name` actualitzat a `file_entity`/`folder_entity`
    - [ ] **Sincronització:** Altres clients veuen el nou nom

#### Cas de Fallada: Operació sense tokens vàlids
- [ ] **Resum:** Intent d'operació quan els tokens han expirat.
- [ ] **Resultat Esperat:**
    - [ ] **API:** Peticions retornen `401 Unauthorized`
    - [ ] **Desktop:** Operacions fallen silenciosament o mostren error
    - [ ] **Token watcher:** Detecta expiració i inicia logout automàtic
    - [ ] **UI:** Es mostra finestra de login

---

## Cas de Prova 29: Operacions de Descàrrega Automàtica

### Cas d'Èxit: Descàrrega automàtica de fitxers nous del servidor
- [ ] **Resum:** Un fitxer afegit al servidor es descarrega automàticament a l'aplicació desktop.
- [ ] **Procés:**
    1. WebSocket rep `SocketResponse` amb nou fitxer
    2. `diff_trees()` detecta `ChangeType::Added` per file
    3. Es crea `Transfer` amb tipus `Download` i estat `Active`
    4. `api::download()` descarrega el fitxer amb streaming
- [ ] **Endpoint utilitzat:** `GET /files/{elementId}/download`
- [ ] **Resultat Esperat:**
    - [ ] **Progress tracking:** Event `transfer` emès amb progrés de descàrrega
    - [ ] **Sistema de fitxers:** Fitxer guardat a la ubicació local corresponent
    - [ ] **Arbre local:** Actualitzat amb `add_node()`
    - [ ] **UI Desktop:** Progress mostrat a "Current transfers"
    - [ ] **Completion:** Transfer marcat com `Completed`

### Cas d'Èxit: Gestió de conflictes de noms en descàrrega
- [ ] **Resum:** Descàrrega de fitxer quan ja existeix un amb el mateix nom localment.
- [ ] **Resultat Esperat:**
    - [ ] **Resolució automàtica:** Sistema genera nom únic afegint sufijo
    - [ ] **Fitxer local:** Es manté sense modificar
    - [ ] **Fitxer descarregat:** Guardat amb nom alternatiu (ex: "document (1).txt")
    - [ ] **Arbre local:** Actualitzat amb ambdós fitxers

---

## Cas de Prova 30: Gestió d'Errors i Recuperació Desktop

### Cas d'Èxit: Gestió d'errors de xarxa durant operacions
- [ ] **Resum:** Operacions de xarxa fallen per problemes de connectivitat.
- [ ] **Scenarios:**
    - Upload fail durant transfer
    - Download interromput
    - API call timeout
- [ ] **Resultat Esperat:**
    - [ ] **Upload fail:**
        - Transfer marcat com fallat (no implementat actualment)
        - Fitxer local es manté
        - Re-intent automàtic en propera sincronització
    - [ ] **Download fail:**
        - Fitxer parcial eliminat
        - Transfer marcat com fallat
        - Re-intent automàtic quan conexió es restauri
    - [ ] **API timeout:**
        - Operació fallida silenciosament
        - Estat local inconsistent temporalment
        - Corregit en próxima sincronització

### Cas d'Èxit: Recuperació després de desconnexió prolongada
- [ ] **Resum:** L'aplicació es reconnecta després d'estar offline.
- [ ] **Procés:**
    1. Aplicació detecta reconnexió WebSocket
    2. Rep arbre actualitzat del servidor
    3. Fa diff amb estat local
    4. Aplica tots els canvis pendents
- [ ] **Resultat Esperat:**
    - [ ] **Sincronització completa:** Tots els canvis del servidor aplicats localment
    - [ ] **Resolució de conflictes:** Sistema gestiona automàticament conflictes
    - [ ] **Estat consistent:** Aplicació torna a estar sincronitzada

---

## Casos de Prova d'Endpoints del Gateway

### Cas de Prova 31: Cobertura Completa d'Endpoints

#### Verificació de tots els endpoints exposats pel Gateway:

**Files Management:**
-  `GET /files/root` - Obtenir carpeta arrel
-  `GET /files/{elementId}` - Obtenir detalls d'element
-  `GET /files/{elementId}/download` - Descarregar element
-  `GET /files/{folderId}/full` - Obtenir contingut complet de carpeta
-  `POST /files` - Crear nova carpeta
-  `POST /upload` - Pujar arxius
-  `PUT /files/{elementId}` - Actualitzar element (rename)
-  `PUT /files/{elementId}/move/{folderId}` - Moure element
-  `DELETE /files/{elementId}` - Eliminar element (redireccionar a trash)
-  `POST /files/{elementId}/copy/{newParentId}` - Copiar element

**File Sharing:**
-  `GET /share/user/{elementId}` - Obtenir usuaris amb accés a element
-  `POST /share` - Compartir element
-  `PUT /share` - Actualitzar permisos de compartició
-  `DELETE /share/{elementId}/user/{username}` - Revocar accés
-  `GET /share/root` - Obtenir elements compartits amb l'usuari

**Trash Service:**
-  `GET /trash/root` - Obtenir contingut de la paperera
-  `GET /files/trash/full` - Vista completa de paperera (redireccionar)
-  `PUT /trash/{elementId}/restore` - Restaurar element
-  `DELETE /trash/{elementId}` - Eliminar permanentment

**User Authentication:**
-  `POST /users/auth/login` - Iniciar sessió
-  `POST /users/auth/register` - Registrar usuari
-  `POST /users/auth/keep-alive` - Renovar token
-  `POST /users/auth/check` - Verificar token
-  `PUT /users/password` - Canviar contrasenya

**User Management:**
-  `GET /users` - Obtenir perfil d'usuari actual
-  `GET /admin/users` - Llistar tots els usuaris (admin)
-  `GET /admin/users/{username}` - Obtenir usuari específic (admin)
-  `PUT /admin/users/{userId}` - Actualitzar usuari (admin)
-  `DELETE /admin/users/{username}` - Eliminar usuari (admin)
-  `GET /users/search` - Cercar usuaris
-  `GET /users/check-email` - Verificar disponibilitat d'email
-  `GET /users/check-username` - Verificar disponibilitat d'username
-  `GET /users/{username}/id` - Obtenir ID d'usuari
-  `PUT /users/profile` - Actualitzar perfil
-  `DELETE /users` - Eliminar compte propi

**WebSocket:**
-  `GET /websocket` - Connexió WebSocket per aplicació desktop
-  `GET /websocket/web` - Connexió WebSocket per aplicació web

**Health Check (utilitzat per aplicació desktop):**
-  `GET /actuator/health` - Verificar estat del servidor (utilitzat en configuració)

---

## Casos de Prova de Funcionalitats Desktop Específiques

### Cas de Prova 32: Sistema de Debouncing Desktop

#### Cas d'Èxit: Agrupació d'operacions múltiples
- [ ] **Resum:** Múltiples canvis ràpids es processen com una sola operació.
- [ ] **Procés:**
    1. File watcher detecta múltiples events en poc temps
    2. Debouncer agrupa events durant 1000ms
    3. S'executa una sola operació de sincronització
- [ ] **Resultat Esperat:**
    - [ ] **Eficiència:** Evita sobrecàrrega d'operacions de xarxa
    - [ ] **Consistència:** Estat final correcte independentment dels events intermedis
    - [ ] **Performance:** Millor rendiment amb operacions massives

### Cas de Prova 33: Comandos Tauri Específics

#### Cas d'Èxit: Forçar sincronització manual
- [ ] **Resum:** L'usuari força una resincronització manual des de la UI desktop.
- [ ] **Procés:**
    1. L'usuari fa clic al botó "Force Sync" a la finestra principal
    2. Es crida `force_sync(app_handle)`
    3. Es para i reinicia el synchronizer
- [ ] **Resultat Esperat:**
    - [ ] **Sincronització:** Tots els canvis pendents es processen
    - [ ] **UI:** Indicador de sincronització activa
    - [ ] **Consistència:** Estat final coherent amb servidor

#### Cas d'Èxit: Obrir carpeta local
- [ ] **Resum:** L'usuari obre la carpeta local sincronitzada des de l'aplicació.
- [ ] **Procés:**
    1. L'usuari fa clic al botó "Open Folder"
    2. Es crida `open_folder(path)`
    3. S'utilitza `opener::open()` per obrir el sistema de fitxers
- [ ] **Resultat Esperat:**
    - [ ] **Sistema:** Explorer/Finder s'obre mostrant la carpeta configurada
    - [ ] **UX:** Accés ràpid als fitxers locals sincronitzats

#### Cas d'Èxit: Verificar estat de connexió
- [ ] **Resum:** L'aplicació verifica constantment l'estat de connexió.
- [ ] **Procés:**
    1. Es crida `check_connection()` periòdicament
    2. Retorna l'estat de `IS_CONNECTED`
- [ ] **Resultat Esperat:**
    - [ ] **UI:** Indicador visual de connexió actualitzat
    - [ ] **Comportament:** Operacions ajustades segons connectivitat

---

### Cas de Prova 4A: Toggle Login/Signup a la Interfície Web

#### Cas d'Èxit: Canvi entre modes de login i signup
- [ ] **Resum:** L'usuari canvia entre els modes de login i signup mitjançant el toggle a la UI.
- [ ] **Procés a la UI:**
    1. Es mostra un toggle fix a la part superior dreta quan no hi ha usuari autenticat
    2. L'usuari fa clic al toggle per canviar entre "Login" i "Signup"
    3. Es fa automàticament redirect a `/login` o `/sign-up` segons el mode
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - Toggle visual amb labels "Login" i "Signup"
        - Canvi automàtic de ruta segons el mode seleccionat
        - Estado del toggle persisteix durant la navegació
        - Redireccions automàtiques si l'usuari està autenticat
    - [ ] **Navegació:** 
        - Mode Login: redirigeix a `/login`
        - Mode Signup: redirigeix a `/sign-up`
        - Redirect automàtic a `/` si l'usuari està autenticat

---

### Cas de Prova 4B: Càrrega Inicial de l'Aplicació

#### Cas d'Èxit: Primer càrrega amb dades existents
- [ ] **Resum:** L'aplicació es carrega inicialment i obté l'estat complet del sistema.
- [ ] **Endpoints utilitzats:**
    - `GET /files/structure` - Estructura completa de carpetes
    - `GET /files/root` - Carpeta arrel
    - `GET /users` - Informació de l'usuari actual
- [ ] **Procés a la UI:**
    1. Es mostra un loader amb missatge "Loading... Please wait while data loads"
    2. Es crida `fetchInitialState()` per carregar dades inicials
    3. Es connecta automàticament al WebSocket si hi ha token vàlid
    4. Es carrega l'estructura de carpetes i el directori actual
- [ ] **Resultat Esperat:**
    - [ ] **API:** 
        - Estructura de carpetes amb tres seccions: root, shared, trash
        - Directori actual amb files i subfolders
        - Informació de l'usuari autenticat
    - [ ] **UI:**
        - Loader desapareix quan es completa la càrrega
        - Es mostra el FileManager amb dades carregades
        - WebSocket connectat per sincronització en temps real
        - Sidebar amb estructura de carpetes expandible
    - [ ] **Store Global:**
        - `fileStore` inicialitzat amb currentDirectory i folderStructure
        - `fileContextStore` amb secció 'root' per defecte
        - Expanded folders restaurades des de sessionStorage
    - [ ] **Persistència:** Directori actual i carpetes expandides recuperades de sessionStorage

#### Cas de Fallada: Error en la càrrega inicial
- [ ] **Resum:** Falla la càrrega de dades inicials per problemes de connectivitat o autenticació.
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - Es manté el loader si hi ha errors de xarxa
        - Redirect a login si el token és invàlid
        - Missatges d'error apropriats via toast
    - [ ] **Store:** Estats inicials per defecte si no es poden carregar dades

---

### Cas de Prova 4C: Sistema de Notificacions i Toasts

#### Cas d'Èxit: Mostrar notificacions d'operacions
- [ ] **Resum:** El sistema mostra notificacions toast per informar l'usuari de l'estat de les operacions.
- [ ] **Tipus de notificacions:**
    - `success`: "File uploaded successfully", "Items moved successfully"
    - `error`: "Error creating folder", "Insufficient permissions"
    - `info`: Missatges informatius generals
    - `warning`: Advertències sobre operacions
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - Toasts apareixen a la cantonada de la pantalla
        - Diferents colors segons el tipus (green=success, red=error)
        - Desapareixen automàticament després d'uns segons
        - Botó per tancar manualment si cal
        - Cua de toasts si n'hi ha múltiples
    - [ ] **Comportament:**
        - No bloquegen la interfície
        - Es mantenen visibles temps suficient per llegir-los
        - Es poden apilar múltiples notificacions

---

## Cas de Prova: Sistema de Navegació i Seccions (UI Web)

### Cas de Prova NA1: Navegació entre Seccions

#### Cas d'Èxit: Canvi entre "Els meus arxius", "Compartit amb mi" i "Paperera"
- [ ] **Resum:** L'usuari navega entre les tres seccions principals de l'aplicació.
- [ ] **Procés a la UI:**
    1. L'usuari fa clic a les seccions del sidebar: Root, Shared, Trash
    2. El `useFileContextStore` actualitza la secció activa
    3. Es carreguen les dades específiques de la secció
    4. Es canvia el contingut del directori actual
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - [ ] **Sidebar:** Secció activa destacada visualment
        - [ ] **Breadcrumbs:** Actualització del path segons la secció
        - [ ] **Contingut:** Files i carpetes específiques de la secció
        - [ ] **Operacions:** Menú contextual adaptat a la secció actual
    - [ ] **Store State:**
        - `fileContextStore.section` actualitzat a 'root'/'shared'/'trash'
        - `currentDirectory` canviat al directori corresponent
        - Selecció i clipboard netejats en canviar secció
    - [ ] **Persistència:** Secció activa guardada a sessionStorage
    - [ ] **API Calls:**
        - `GET /files/root` per secció root
        - `GET /share/root` per secció shared  
        - `GET /trash/root` per secció trash

#### Cas de Fallada: Error carregant secció
- [ ] **Resum:** Falla la càrrega de dades d'una secció específica.
- [ ] **Resultat Esperat:**
    - [ ] **UI:** Toast d'error "Could not load initial files"
    - [ ] **Store:** Es manté a la secció anterior si falla el canvi

### Cas de Prova NA2: Expansió de Carpetes al Sidebar

#### Cas d'Èxit: Expandir/Col·lapsar carpetes
- [ ] **Resum:** L'usuari expandeix i col·lapsa carpetes al sidebar per navegar l'estructura.
- [ ] **Procés a la UI:**
    1. L'usuari fa clic a la fletxa d'expansió al costat d'una carpeta
    2. Es crida `toggleFolderExpansion(folderId, isExpanded)`
    3. S'actualitza l'estat d'expansió a l'store
    4. Es carreguen subcarpetes si no estaven carregades
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - [ ] **Animació:** Expansió/col·lapse suau de les subcarpetes
        - [ ] **Icona:** Fletxa que rota segons l'estat (expandit/col·lapsat)
        - [ ] **Lazy Loading:** Subcarpetes es carreguen només quan s'expandeixen
    - [ ] **Store:**
        - `expandedFolders` array actualitzat amb IDs de carpetes expandides
        - Estat aplicat recursivament a l'estructura de carpetes
    - [ ] **Persistència:** 
        - Estat d'expansió guardat a sessionStorage
        - Es restaura entre sessions de l'usuari

### Cas de Prova NA3: Filtres i Ordenació de Contingut

#### Cas d'Èxit: Aplicar filtres per tipus d'element
- [ ] **Resum:** L'usuari filtra el contingut per mostrar només fitxers o només carpetes.
- [ ] **Opcions de filtre:**
    - [ ] **filterByFiles**: Oculta/mostra fitxers
    - [ ] **filterByFolders**: Oculta/mostra carpetes
- [ ] **Procés a la UI:**
    1. L'usuari activa/desactiva els controls de filtre
    2. Es crida `setFilterByFiles()` o `setFilterByFolders()`
    3. La funció `filterAndSort()` aplica els filtres
    4. Es re-renderitza la graella amb elements filtrats
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - Elements filtrats desapareixen/apareixen immediatament
        - Controls de filtre reflecteixen l'estat actual
        - Graella es reorganitza segons els elements visibles
    - [ ] **Store:** `fileContextStore` actualitzat amb els filtres actius

#### Cas d'Èxit: Ordenació per diferents criteris
- [ ] **Resum:** L'usuari ordena els elements per nom, data, mida o tipus.
- [ ] **Opcions d'ordenació:**
    - [ ] **Nom:** A-Z (`nameUp`) / Z-A (`nameDown`)
    - [ ] **Data creació:** Ascendent (`createdAtUp`) / Descendent (`createdAtDown`)
    - [ ] **Data modificació:** Ascendent (`updatedAtUp`) / Descendent (`updatedAtDown`)
    - [ ] **Mida:** Petit a gran (`sizeUp`) / Gran a petit (`sizeDown`)
    - [ ] **Tipus:** Carpetes primer, després fitxers (`type`)
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - Elements reordenats immediatament segons el criteri
        - Indicator visual del criteri d'ordenació actiu
        - Ordenació consistent en totes les vistes
    - [ ] **Store:** `sortBy` actualitzat amb el criteri seleccionat

---

## Casos de Prueba de Funcionalidades Específicas Avanzadas

### Cas de Prova ADV1: Upload de Carpeta Completa (UI Web)

#### Cas d'Èxit: Upload de estructura de carpetes amb webkitdirectory
- [ ] **Resum:** L'usuari puja una carpeta sencera amb subcarpetes i arxius mitjançant la funcionalitat webkitdirectory.
- [ ] **Procés a la UI:**
    1. L'usuari fa clic al botó "Upload Folder" que activa `webkitdirectory`
    2. Es selecciona una carpeta del sistema de fitxers
    3. Es construeix recursivament l'estructura `UploadedTree`
    4. Es creen primer les carpetes, després es pugen els arxius
- [ ] **Validacions:**
    - Verificació que les carpetes es creen en l'ordre correcte (parent abans que children)
    - Validació de noms de carpetes i arxius segons regles del sistema
    - Comprovació de permisos de creació a cada nivell
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - [ ] **Actualització optimista:** Tota l'estructura apareix immediatament a la interfície
        - [ ] **Progress múltiple:** Indicador de progrés per cada arxiu que es puja
        - [ ] **Error handling:** Si una carpeta falla, es manté l'estructura parcial creada
        - Toast d'èxit "Folder uploaded successfully" quan completa
    - [ ] **API:** Múltiples crides `POST /files` per carpetes i `POST /upload` per arxius
    - [ ] **Base de dades:** Estructura completa replicada amb `element_entity`, `folder_entity`, `file_entity`
    - [ ] **Sincronització:** Aplicacions desktop reben tota l'estructura via WebSocket

### Cas de Prova ADV2: Selecció per Drag amb Selecto.js (UI Web)

#### Cas d'Èxit: Selecció múltiple arrossegant rectangle de selecció
- [ ] **Resum:** L'usuari selecciona múltiples elements dibuixant un rectangle de selecció.
- [ ] **Procés a la UI:**
    1. L'usuari fa clic i arrossega sobre un àrea buida per crear rectangle de selecció
    2. El `useSelecto` hook gestiona els events `handleMouseDown`, `handleMouseMove`, `handleMouseUp`
    3. Elements dins del rectangle es marquen com seleccionats progressivament
- [ ] **Resultat Esperat:**
    - [ ] **UI:**
        - [ ] **Visual feedback:** Rectangle de selecció semi-transparent es dibuixa en temps real
        - [ ] **Selecció progressiva:** Elements s'afegeixen/treuen de la selecció mentre s'arrossega
        - [ ] **Intersecció:** Només elements que intersecten amb el rectangle són seleccionats
        - [ ] **Compatibilitat:** Funciona en combinació amb Ctrl+Click i Shift+Click
    - [ ] **Store:** `selectedFiles` i `selectedFilesIds` actualitzats amb elements del rectangle
    - [ ] **Keyboard:** Shortcuts funcionen amb la selecció resultant

#### Cas de Fallada: Selecció durant menú contextual obert
- [ ] **Resum:** Intent de fer selecció per drag quan el menú contextual està obert.
- [ ] **Resultat Esperat:**
    - [ ] **UI:** Selecció per drag deshabilitada quan `contextMenuOpen: true`
    - [ ] **Comportament:** El menú es tanca si es fa clic fora i després es permet selecció

### Cas de Prova ADV3: Comportament Visual del Clipboard (UI Web)

#### Cas d'Èxit: Operacions de copy/cut amb feedback visual
- [ ] **Resum:** L'usuari utilitza Ctrl+C i Ctrl+X amb indicacions visuals clares del clipboard.
- [ ] **Procés a la UI:**
    1. L'usuari selecciona elements i prem Ctrl+C (copy) o Ctrl+X (cut)
    2. Elements es guarden al `clipboard` del `useFileSelectionStore`
    3. Es mostra feedback visual diferent per copy vs cut
- [ ] **Resultat Esperat:**
    - [ ] **Copy (Ctrl+C):**
        - Elements copiats es mantenen amb aparença normal
        - `isCut: false` al store
        - Elements romanen seleccionats
    - [ ] **Cut (Ctrl+X):**
        - Elements tallats es mostren semi-transparents o grisats
        - `isCut: true` al store
        - Elements es deseleccionen (`selectedFiles: []`)
        - Visual persistent fins que es fa paste o es neteja clipboard
    - [ ] **Paste (Ctrl+V):**
        - Operació apropiada segons `isCut` (move vs copy)
        - Elements cortats recuperen aparença normal després de move
        - Clipboard es neteja després de paste de cut operation

#### Cas d'Èxit: Gestió de clipboard entre navegació
- [ ] **Resum:** El clipboard es manté o es neteja adequadament quan l'usuari navega entre carpetes.
- [ ] **Resultat Esperat:**
    - [ ] **Navegació mateix parent:** Clipboard es manté
    - [ ] **Navegació diferent parent:** Clipboard es manté per permitre move/copy entre carpetes
    - [ ] **Canvi de secció:** Clipboard es neteja en canviar entre root/shared/trash

### Cas de Prova ADV4: Sistema de Debouncing Desktop

#### Cas d'Èxit: Agrupació d'events múltiples amb debouncer
- [ ] **Resum:** El file watcher del desktop agrupa múltiples events ràpids per evitar sobrecàrrega.
- [ ] **Procés a l'Aplicació Desktop:**
    1. Múltiples files es modifiquen/creen/eliminen en ràpida successió
    2. El debouncer aplica un delay de 1000ms per agrupar events
    3. S'executa una sola operació de sincronització amb tots els canvis
- [ ] **Escenaris típics:**
    - Copy/paste de múltiples fitxers simultàniament
    - Operacions de move de carpetes amb molts continguts
    - Descompressió d'arxius ZIP amb molts fitxers
- [ ] **Resultat Esperat:**
    - [ ] **Performance:** 
        - Una sola operació de xarxa per grup d'events relacionats
        - Evita flood de peticions HTTP consecutives
        - Millor rendiment en operacions massives
    - [ ] **Consistència:**
        - Estat final correcte independentment del nombre d'events intermedis
        - Ordre de operacions preservat quan és important
    - [ ] **Logs:** Events agrupats visibles en debug mode

### Cas de Prova ADV5: System Tray i Gestió de Finestres Desktop

#### Cas d'Èxit: Interacció completa amb system tray
- [ ] **Resum:** L'usuari gestiona l'aplicació a través del system tray icon.
- [ ] **Opcions del menú tray:**
    - [ ] **Show:** Obre/enfoca la finestra principal
    - [ ] **Configuration:** Obre finestra de configuració
    - [ ] **Quit:** Tanca completament l'aplicació
- [ ] **Procés:**
    1. L'aplicació es minimitza al system tray quan es tanca la finestra
    2. L'usuari fa clic dret a l'icona del tray per accedir al menú
    3. L'usuari selecciona opcions del menú
- [ ] **Resultat Esperat:**
    - [ ] **System Tray:**
        - Icona persistent visible al system tray
        - Left click: obre finestra principal
        - Right click: mostra menú contextual
    - [ ] **Gestió de finestres:**
        - [ ] **Main window:** TopRight position, always on top, perd focus quan es clica fora
        - [ ] **Config window:** Centred, modal behavior
        - [ ] **Login window:** Centred, blocking mode
        - [ ] **Initial config:** Centred, setup mode
    - [ ] **Lifecycle:** Aplicació no es tanca quan es tanquen finestres, només amb "Quit"

#### Cas d'Èxit: Navegació automàtica entre finestres segons estat
- [ ] **Resum:** L'aplicació determina quina finestra mostrar segons l'estat de configuració i autenticació.
- [ ] **Lògica de decisió:**
    1. Si `!config.is_configured`: mostra Initial Configuration
    2. Si no hi ha credencials vàlides: mostra Login
    3. Si tot és vàlid: mostra Main + inicia sincronització
- [ ] **Resultat Esperat:**
    - [ ] **Flux automàtic:** Usuari és dirigit a la finestra apropiada sense intervenció manual
    - [ ] **Persistència:** Decisions basades en `config.json` i tokens guardats

### Cas de Prova ADV6: Progress Tracking Detallat de Transfers

#### Cas d'Èxit: Tracking complet d'upload amb streaming
- [ ] **Resum:** Upload de fitxer gran amb progress tracking en temps real.
- [ ] **Procés a l'Aplicació Desktop:**
    1. File watcher detecta fitxer nou (>1MB)
    2. Es crea `Transfer` object amb `TransferType::Upload` i `TransferState::Active`
    3. Upload s'inicia amb streaming i progress callbacks
    4. Progress events s'emeten cada x% de completació
- [ ] **Resultat Esperat:**
    - [ ] **UI Desktop:**
        - Transfer apareix immediatament a la llista "Current transfers"
        - Progress bar s'actualitza en temps real (0-100%)
        - Speed i ETA calculats i mostrats
        - Botó Cancel disponible durant upload actiu
    - [ ] **Events emesos:**
        - `transfer` event amb `{ id, progress, speed, eta, state }`
        - `transfer_completed` event quan finalitza amb èxit
        - `transfer_failed` event si hi ha errors
    - [ ] **Estats possibles:**
        - `Active`: Transfer en curs
        - `Completed`: Transfer finalitzat amb èxit
        - `Failed`: Transfer fallat amb error message
        - `Cancelled`: Transfer cancel·lat per l'usuari

#### Cas d'Èxit: Download automàtic amb progress tracking
- [ ] **Resum:** Fitxer compartit es descarrega automàticament amb progress visible.
- [ ] **Procés:**
    1. WebSocket rep notificació de nou fitxer compartit
    2. Es crea `Transfer` object amb `TransferType::Download`
    3. Download s'inicia amb streaming i escriptura incremental
- [ ] **Resultat Esperat:**
    - [ ] **Progress visual:** Idèntic a upload però per download
    - [ ] **File system:** Fitxer apareix progressivament al sistema local
    - [ ] **Fallback:** Si download falla, es reintenta automàticament

### Cas de Prova ADV7: Gestió Avançada de Conflictes de Noms

#### Cas d'Èxit: Resolució automàtica de noms duplicats
- [ ] **Resum:** El sistema gestiona automàticament conflictes de noms en operacions de fitxers.
- [ ] **Algoritme de resolució:**
    - Per fitxers: `document.txt` → `document (1).txt` → `document (2).txt`
    - Per carpetes: `folder` → `folder (1)` → `folder (2)`
    - Manté extensions de fitxer intactes
- [ ] **Aplicat en:**
    - Copy/paste amb nom duplicat
    - Download de fitxer quan ja existeix localment
    - Move/rename amb conflicte
- [ ] **Resultat Esperat:**
    - [ ] **UI Web:**
        - Operació continua sense bloquejar amb nom generat automàticament
        - Toast informatiu "File renamed to avoid conflict"
        - Nom final visible immediatament
    - [ ] **Desktop:**
        - Fitxers locals conserven noms originals
        - Fitxers descarregats reben noms alternatius automàticament
    - [ ] **Base de dades:** Elements guardats amb noms únics generats

### Cas de Prova ADV8: Reconexió Automàtica WebSocket Detallada

#### Cas d'Èxit: Reconexió amb backoff exponential
- [ ] **Resum:** WebSocket es reconnecta automàticament amb estratègia de backoff exponential.
- [ ] **Estratègia de reconexió:**
    - [ ] **Web UI:** Fins a 10 intents amb delays: 1s, 2s, 4s, 8s, 16s, 30s (màxim)
    - [ ] **Desktop:** Loop infinit amb 5s fix entre intents
- [ ] **Procés detallat:**
    1. Connexió WebSocket es perd (xarxa, servidor reinicia, token expira)
    2. `onclose` event detectat
    3. Si `shouldReconnect = true`, s'inicia sequence de reconexió
    4. Cada intent incrementa `reconnectAttempts` i calcula nou delay
- [ ] **Resultat Esperat:**
    - [ ] **Web UI:**
        - [ ] **Logs:** "Attempting to reconnect in Xms (attempt Y/10)"
        - [ ] **Max attempts:** Després de 10 intents, atura reconexió automàtica
        - [ ] **Reset:** Si reconnecta amb èxit, reset de `reconnectAttempts = 0`
    - [ ] **Desktop:**
        - [ ] **Persistent:** Continua intentant indefinidament
        - [ ] **UI Feedback:** `IS_CONNECTED = false` i overlay "Connection Lost"
        - [ ] **Recovery:** Quan reconnecta, sincronització completa automàtica
    - [ ] **Token expiry:** Si reconexió falla per token invàlid, inicia logout automàtic

#### Cas d'Èxit: Sincronització completa després de reconnexió
- [ ] **Resum:** Després de reconnectar, l'aplicació sincronitza tots els canvis pendents.
- [ ] **Procés de recovery:**
    1. WebSocket reconnecta amb èxit
    2. Aplicació rep arbre actualitzat del servidor
    3. Es fa diff amb estat local per detectar tots els canvis
    4. S'apliquen automàticament tots els canvis perduts
- [ ] **Resultat Esperat:**
    - [ ] **Desktop:** 
        - Tots els fitxers nous del servidor es descarreguen
        - Tots els canvis locals pendents es pugen
        - Conflictes es resolen automàticament
    - [ ] **Web UI:** 
        - Vista es refresca completament amb dades actualitzades
        - Operacions pendents es reintenten si cal
    - [ ] **Consistència:** Estat final idèntic a si no hagués hagut desconnexió

---

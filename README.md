# FlowLabs Roleplay: Launcher

Launcher Android do **FlowLabs Roleplay**, do **FlowLabs Studio**.
© 2026 FlowLabs Studio. Todos os direitos reservados.

> O FlowLabs Roleplay não é afiliado à Rockstar Games nem à Take-Two Interactive.
> O launcher **não distribui arquivos do GTA San Andreas**. Cada jogador usa a própria cópia original.

## Como compilar

O projeto foi feito para o **AIDE** (Java puro, sem bibliotecas externas), mas também abre no Android Studio.
Também dá para montar o APK sem Gradle: `aapt` → `javac` → `dx` → `zipalign` → `apksigner`.

- Pacote: `com.nevesware.flowlabsrp`
- Android mínimo: 5.0 (API 21), alvo: API 29
- Tela em paisagem e tela cheia (modo jogo)

No AIDE: abra a pasta `flowlabs-roleplay` e toque em **Run**.

## Visual

Estilo de launcher de jogo: arte da cidade em tela cheia, logo no centro, atalhos nos cantos e o botão **JOGAR**.
**Nenhuma tela tem rolagem.** Servidores, loja, novidades e configurações abrem em painéis de vidro por cima da tela.

- Fonte: **Inter** (licença livre OFL, em `assets/fonts`).
- Cores: noite `#07080F`, violeta `#7C4DFF`, magenta `#E0457B`, âmbar `#FFB547`. O botão principal usa o degradê de pôr do sol âmbar → magenta.
- Arte: `res/drawable-nodpi/bg_city.jpg` (fundo) e `loading.jpg` (tela de carregamento "FlowLabs Studio apresenta").
- Ícone: `res/drawable/ic_launcher.png`. As versões para a loja ficam em `store/` (`icon-512.png` e `feature-1024x500.jpg`).
- Ícones da interface: vetores em `res/drawable/ic_*.xml`.

## Telas

| Tela | Recursos |
|---|---|
| **Carregamento** | Arte do FlowLabs Studio, barra de progresso com etapas e copyright |
| **Login** | Abas Entrar / Criar conta. Conta local (nome RP `Nome_Sobrenome`, e-mail e senha com hash PBKDF2), **Google** e **Discord** (via Supabase), convidado e recuperação de senha |
| **Principal** | Perfil, Loja VIP, Novidades, Configurações, redes sociais, seletor de servidor com ping, status dos arquivos e **JOGAR** |
| **Servidores** (painel) | Online / offline / em breve, ping, vagas e tags. Toque para escolher |
| **Novidades** (painel) | Notícias da cidade |
| **Loja VIP** (painel) | Planos Bronze, Prata e Ouro |
| **Configurações** (painel) | Abas: Gráficos (qualidade, FPS), Jogo (voz, notificações, vibração), **Arquivos** (importar, verificar, relatório, remover), Conta (perfil, estatísticas, trocar nome, sair) e Sobre (versão, copyright, termos) |

## Onde mudar as coisas

Quase tudo fica em **`app/src/main/java/com/nevesware/flowlabsrp/Config.java`**:

- links (Discord, Instagram, TikTok, site, termos, loja);
- `SUPABASE_URL` e `SUPABASE_ANON_KEY` (login com Google e Discord);
- `ALLOW_GUEST`, que liga ou desliga o botão de convidado;
- `SERVERS_URL`, `NEWS_URL` e `UPDATE_URL`, que apontam para JSONs no site (servidores, notícias e atualizações sem precisar lançar um APK novo);
- lista embutida de servidores e notícias.

As cores ficam em `ui/Ui.java`.

### Ativar o login com Google e Discord (Supabase)

1. Crie o projeto no Supabase.
2. Em **Authentication → Providers**, ative o **Google** e o **Discord** (cada um pede o Client ID e o Secret do respectivo console).
3. Em **Authentication → URL Configuration → Redirect URLs**, adicione `flowlabsrp://auth/callback`.
4. Coloque a Project URL e a chave anon em `Config.java`.

O fluxo já está pronto em `auth/OAuth.java`: o launcher abre o navegador, o Supabase devolve para `flowlabsrp://auth/callback` e o app busca os dados do usuário.

## Estrutura do código

```
com.nevesware.flowlabsrp
├── Config.java            ← marca, links, Supabase, servidores, notícias
├── Settings.java          ← ajustes do jogador
├── SplashActivity.java    ← abertura
├── AuthActivity.java      ← login / criar conta / Google / Discord / convidado
├── MainActivity.java      ← painel: início, servidores, arquivos, loja, ajustes, perfil
├── auth/
│   ├── Session.java       ← sessão logada
│   ├── AccountStore.java  ← contas locais (trocar pelo Supabase depois)
│   └── OAuth.java         ← Google/Discord via Supabase
├── game/
│   ├── GameFiles.java     ← pasta do jogo, verificação, relatório
│   ├── GameImporter.java  ← cópia dos arquivos do jogador (seletor de pastas)
│   └── GameLauncher.java  ← verificações antes de jogar (cliente vem aqui)
├── net/
│   ├── ServerInfo.java, NewsItem.java
│   ├── ServerPinger.java  ← ping dos servidores
│   ├── RemoteContent.java ← servers.json / news.json / update.json
│   └── Http.java
└── ui/Ui.java             ← tema e componentes
```

## Arquivos do jogo

- O jogador copia a pasta do **GTA SA original dele** para um lugar acessível (ex.: `Download/GTA-SA`) e escolhe essa pasta no launcher.
  A cópia é necessária porque, a partir do Android 11, um app não pode ler `Android/data` e `Android/obb` de outro app.
- Os arquivos vão para `Android/data/com.nevesware.flowlabsrp/files/gta/`.
- A lista de itens obrigatórios em `GameFiles.REQUIRED` é **provisória**. Depois de importar uma instalação real, toque em **Relatório** (ele lista só nomes e tamanhos, sem conteúdo) e ajuste a lista.
- **Nunca** faça o launcher baixar os arquivos do jogo de um servidor: isso seria distribuir o jogo.

## Próximas etapas

1. **Cliente multiplayer** (`game/GameLauncher.java`): carregar o motor do GTA SA do próprio jogador e aplicar os hooks do FlowLabs (sincronização, chat, HUD).
2. **Servidor FlowLabs**: Linux, UDP, contas, scripts em Lua.
3. **Supabase**: contas, perfis (nome RP único), estatísticas, whitelist.
4. Trocar `AccountStore` (local) pelas chamadas do Supabase e desligar `ALLOW_GUEST`.

# Transformação do Metrolist: YouTube Music → Subsonic/Navidrome Player

## 📋 Resumo das Alterações

Esta é uma conversão em andamento do aplicativo Metrolist de um player do YouTube Music para um player compatível com servidores Subsonic e Navidrome.

## ✅ O que já foi implementado

### 1. **Módulo Subsonic API (COMPLETO)** ✓
- **Localização**: `subsonic/src/main/kotlin/com/metrolist/subsonic/`
- **Arquivos criados**:
  - `Subsonic.kt` - Cliente principal da API
  - `models/SubsonicModels.kt` - Modelos de dados

**Funcionalidades implementadas**:
- ✅ Autenticação MD5 com salt (compatível com Subsonic API 1.16.1)
- ✅ Busca de músicas, álbuns e artistas (`search3`)
- ✅ Listagem de artistas (`getArtists`)
- ✅ Detalhes de artista e álbum
- ✅ Músicas aleatórias (`getRandomSongs`)
- ✅ Gerenciamento de playlists (criar, atualizar, deletar)
- ✅ Sistema de favoritos (star/unstar)
- ✅ URLs de streaming com autenticação
- ✅ URLs de cover art

### 2. **Tela de Configurações** ✓
- **Localização**: `app/src/main/kotlin/com/metrolist/music/ui/screens/settings/SubsonicSettings.kt`

**Funcionalidades**:
- ✅ Interface para configurar servidor Subsonic/Navidrome
- ✅ Campos para URL, username, password
- ✅ Botão de teste de conexão
- ✅ Armazenamento seguro em DataStore
- ✅ Switch para ativar/desativar Subsonic
- ✅ Configuração de bitrate máximo

### 3. **Integração no App** ✓
- ✅ Adicionado módulo `subsonic` ao `settings.gradle.kts`
- ✅ Dependência adicionada ao `app/build.gradle.kts`
- ✅ Chaves de preferências criadas (`SubsonicServerUrlKey`, `SubsonicUsernameKey`, etc.)
- ✅ Strings localizadas adicionadas (pt-BR/en)
- ✅ Rota de navegação configurada (`settings/subsonic`)
- ✅ Item adicionado ao menu de configurações
- ✅ Inicialização automática do Subsonic no `App.kt`
- ✅ Observação de mudanças nas configurações em tempo real

## 🚧 O que ainda precisa ser feito

### 4. **Adaptação da Camada de Dados** ✓ (COMPLETO)
- ✅ Funções de extensão criadas (SubsonicExtensions.kt)
- ✅ SubsonicRepository implementado
- ✅ Mappers para converter modelos Subsonic → Room entities
- ✅ Suporte a sincronização de dados do servidor

### 5. **Player e Playback Service** ✓ (COMPLETO)
- ✅ `SubsonicPlayerUtils` criado (equivalente ao `YTPlayerUtils`)
- ✅ `MusicService` modificado para reproduzir streams Subsonic
- ✅ Suporte a diferentes formatos de áudio
- ✅ Sistema de detecção automática (Subsonic vs YouTube)

### 6. **ViewModels e UI**
- [ ] Atualizar `HomeViewModel` para buscar dados do Subsonic
- [ ] Modificar `SearchViewModel` para usar `Subsonic.search()`
- [ ] Atualizar telas de Álbuns, Artistas, Playlists
- [ ] Implementar sincronização de biblioteca
- [ ] Adicionar suporte a cover arts do Subsonic

### 7. **Limpeza do Código**
- [ ] Remover ou tornar opcional o módulo `innertube`
- [ ] Limpar imports do YouTube
- [ ] Criar modo híbrido (opcional: YouTube + Subsonic)

### 8. **Recursos e Branding**
- [ ] Atualizar nome do app (se desejado)
- [ ] Modificar ícones relacionados ao YouTube
- [ ] Atualizar strings de ajuda e tutorial
- [ ] Atualizar README.md

### 9. **Testes**
- [ ] Testar conexão com Navidrome
- [ ] Testar conexão com Subsonic
- [ ] Testar streaming de áudio
- [ ] Testar sincronização de playlists
- [ ] Testar busca e navegação

## 🔧 Estrutura Técnica

### Dependências Adicionadas
```kotlin
// subsonic/build.gradle.kts
- io.ktor:ktor-client-core
- io.ktor:ktor-client-okhttp
- io.ktor:ktor-client-content-negotiation
- io.ktor:ktor-serialization-kotlinx-json
```

### Chaves de Preferências
```kotlin
SubsonicServerUrlKey       // URL do servidor
SubsonicUsernameKey        // Nome de usuário
SubsonicPasswordKey        // Senha (armazenada com segurança)
SubsonicEnabledKey         // Ativar/desativar
SubsonicMaxBitRateKey      // Bitrate máximo para streaming
```

### API Subsonic Implementada
- Versão da API: **1.16.1**
- Compatibilidade: **Subsonic, Navidrome, Airsonic, OpenSubsonic**
- Autenticação: **Token-based (MD5 + salt)**

## 📝 Próximos Passos Recomendados

1. **Testar a configuração do servidor**
   - Compile o projeto
   - Execute no emulador/dispositivo
   - Navegue para Configurações → Subsonic/Navidrome
   - Configure seu servidor e teste a conexão

2. **Implementar adaptação de dados**
   - Começar com a camada de Repository
   - Criar mappers entre modelos Subsonic e entidades Room
   - Implementar sincronização de biblioteca

3. **Adaptar o player**
   - Modificar o `PlayerService` para aceitar URLs Subsonic
   - Implementar streaming com autenticação
   - Adicionar cache de áudio

## 🌟 Recursos do Subsonic/Navidrome

### Suportados pela API implementada:
- ✅ Streaming de música
- ✅ Busca de música, álbum, artista
- ✅ Playlists
- ✅ Favoritos (starred)
- ✅ Cover art
- ✅ Informações de álbum/artista
- ✅ Músicas aleatórias
- ✅ Álbuns recentes
- ✅ Controle de bitrate

### Possíveis extensões futuras:
- [ ] Podcasts
- [ ] Radio internet
- [ ] Scrobbling (já existe LastFM)
- [ ] Compartilhamento
- [ ] Ratings/avaliações
- [ ] Letras de música (se disponível no servidor)

## 🔐 Segurança

- Senha armazenada em DataStore (criptografado no Android)
- Autenticação via token MD5 (não envia senha em texto claro)
- Suporte a HTTPS recomendado
- Salt aleatório em cada requisição

## 📚 Documentação de Referência

- [Subsonic API Documentation](http://www.subsonic.org/pages/api.jsp)
- [Navidrome Documentation](https://www.navidrome.org/)
- [OpenSubsonic API](https://opensubsonic.netlify.app/)

---

**Status do Projeto**: 🟡 Em desenvolvimento ativo
**Progresso**: ~50% completo
**Última atualização**: 2025-10-01

<div align="center">
  <img src=".github/images/AppLogo_v1.png" alt="HonNoMachi Logo" width="200"/>

# 📚 HonNoMachi (本の街 - Grad knjiga)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-Educational-lightgrey.svg)]()

**HonNoMachi** je Android aplikacija namijenjena ljubiteljima knjiga. Omogućuje registriranim korisnicima da postanu dio zajednice u kojoj mogu prodavati knjige koje im više ne trebaju i otkrivati nove naslove za svoju kolekciju.

[📖 Dokumentacija](https://25-26-izvanredni-tim.atlassian.net/wiki/spaces/HNMT/overview) • [🐛 Prijavi Bug]() • [📋 Product Backlog List](https://25-26-izvanredni-tim.atlassian.net/jira/software/projects/HNM/list)
</div>

---

## Sadržaj

- [O projektu](#-o-projektu)
- [Tech Stack](#-tech-stack)
- [Preduvjeti](#-preduvjeti)
- [Instalacija](#-instalacija)
- [Funkcionalnosti](#-funkcionalnosti)
- [Contributing](#-contributing)
- [DevOps](#%EF%B8%8F-devops)
- [Dokumentacija](#-dokumentacija)
- [Tim](#-tim)

---

## O projektu

HonNoMachi (本の街 - "Grad knjiga") je mobilna platforma koja spaja kupce i prodavače rabljenih knjiga. Aplikacija pruža intuitivno korisničko iskustvo s fokusom na sigurnost transakcija i jednostavnost korištenja.

**Ključne značajke:**
- Sigurna autentifikacija korisnika (Email/Lozinka, Google OAuth)
- Pretraživanje i filtriranje knjiga po nazivu, žanru i autoru
- Košarica s integriranim plaćanjem
- Simulacija plaćanja putem Stripe integracije
- Upravljanje korisničkim profilom
- Administratorski panel za moderaciju

---

## Tech Stack

| Kategorija               | Tehnologija                                           |
|--------------------------|-------------------------------------------------------|
| **Jezik**                | Kotlin                                                |
| **UI Framework**         | Jetpack Compose + Material Design 3                   |
| **Arhitektura**          | MVVM (Model-View-ViewModel)                           |
| **Backend**              | Firebase (Authentication, Firestore, Cloud Functions) |
| **Plaćanje**             | Stripe Android SDK (simulacija)                       |
| **Async Operations**     | Kotlin Coroutines + Flow                              |
| **Dependency Injection** | Manual / Hilt (planirano)                             |
| **Version Control**      | Git / GitHub (Git Flow workflow)                      |
| **CI/CD**                | GitHub Actions ([dokumentacija](docs/CICD_DEVOPS.md)) |
| **Project Management**   | Jira + Confluence                                     |

---

## Preduvjeti

Prije nego započnete s instalacijom, provjerite imate li sljedeće:

- **Android Studio** Ladybug (2024.2.1) ili novije
- **JDK** 17 ili novije
- **Android SDK** s minimalno API 24 (Android 7.0 Nougat)
- **Git** instaliran na računalu
- **Firebase projekt** (ili pristup postojećem projektu tima)
- **Google Play Services** na uređaju/emulatoru

---

## Instalacija

### 1. Kloniranje repozitorija

```bash
git clone https://github.com/25-26-izvanredni-tim/HonNoMachi.git
cd HonNoMachi
```

### 2. Firebase konfiguracija

> **Važno:** Datoteka `google-services.json` nije uključena u repozitorij zbog sigurnosnih razloga.

**Opcija A:** Zatražite datoteku od člana tima putem sigurnog kanala.

**Opcija B:** Preuzmite iz Firebase konzole:
1. Prijavite se na [Firebase Console](https://console.firebase.google.com/)
2. Odaberite projekt **HonNoMachi**
3. Idite na Project Settings → Your apps → Android app
4. Preuzmite `google-services.json`
5. Premjestite datoteku u `app/` direktorij projekta

### 3. Sinkronizacija i pokretanje

1. Otvorite projekt u Android Studiju
2. Kliknite **Sync Now** za sinkronizaciju Gradle datoteka
3. Povežite Android uređaj ili pokrenite emulator
4. Kliknite **Run 'app'** ili koristite `Shift + F10`

### Detaljne upute

Za detaljnije upute o postavljanju projekta, pogledajte:
**[Development Setup - Confluence](https://25-26-izvanredni-tim.atlassian.net/wiki/x/DIDE)**

---

## 📱 Funkcionalnosti

### Implementirano (Sprint 01)

#### Autentifikacija korisnika
| Funkcionalnost                |  Opis                                                |
|-------------------------------|-----------------------------------------------------|
| Registracija (Email/Lozinka)  | Kreiranje računa s validacijom podataka             |
| Email verifikacija            | Firebase Authentication verifikacijski tok          |
| Prijava/Odjava                | Sigurna autentifikacija postojećih korisnika        |
| Validacija forme              | Real-time provjera email formata i politike lozinke |
| Ponovno slanje verifikacije   | Opcija za slanje novog verifikacijskog emaila       |
| Pohrana korisnika (Firestore) | Spremanje korisničkih podataka u bazu               |

### Planirano (Sprint 02+)

#### Autentifikacija
- [ ] Google OAuth prijava (Gmail račun)
- [ ] Reset lozinke (zaboravljena lozinka)

#### Upravljanje knjigama
- [ ] Pregled svih dostupnih knjiga (HomePage)
- [ ] Pretraga i filtriranje (naziv, žanr, autor)
- [ ] Detaljni pregled pojedine knjige
- [ ] Kreiranje nove ponude knjige za prodaju
- [ ] Upravljanje vlastitim ponudama (aktivne/neaktivne)

#### Kupovina
- [ ] Dodavanje knjiga u košaricu
- [ ] Pregled košarice s izračunom ukupnog iznosa
- [ ] Stripe integracija za simulaciju plaćanja
- [ ] Potvrda narudžbe i sažetak plaćanja
- [ ] Portfelj kupljenih knjiga (Moja knjižnica)

#### Korisnički profil
- [ ] Ažuriranje podataka (ime, kontakt, adresa)
- [ ] Promjena lozinke

#### Administrator panel
- [ ] Pregled i upravljanje korisnicima
- [ ] Suspenzija/reaktivacija korisničkih računa
- [ ] Pregled i upravljanje svim knjigama u ponudi

### Sprint 03

### Sprint 04

### Sprint 05

### Sprint 06

### Sprint 07

---

## Contributing

Projekt koristi **Git Flow** workflow sa sljedećom strukturom grana:

### Workflow
1. Kreirajte novu granu iz `develop`
2. Implementirajte promjene
3. Kreirajte Pull Request prema `develop`
4. Zatražite code review od barem jednog člana tima
5. Nakon odobrenja, merge u `develop`

![GitFlow_example_diagram.png](.github/images/GitFlow_example_diagram.png)

---

## DevOps

Projekt koristi DevOps prakse za automatizaciju procesa razvoja, testiranja i isporuke. Detaljnija dokumentacija dostupna je u [CI/CD i DevOps](docs/CICD_DEVOPS.md).

### 1. Verzioniranje programskog koda

- [x] **Git** kao sustav za kontrolu verzija s **Git Flow** workflow strategijom
- [x] **GitHub** kao udaljeni repozitorij i platforma za suradnju
- [x] Struktura grana: `master` (produkcija), `develop` (razvoj), `feature/*`, `bugfix/*`, `release/*`
- [x] Obvezni **Pull Requesti** s code reviewom prije merga u `develop`

### 2. Izgradnja

- [x] **Gradle (Kotlin DSL)** kao sustav za izgradnju projekta
- [x] Automatska izgradnja **Debug APK-a** unutar CI pipeline-a na svakom push-u i PR-u
- [x] Automatska izgradnja **Release APK-a** pri push-u na `master` granu
- [x] Konfiguracija: `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`, `JDK 17`

### 3. Kontinuirana integracija (CI)

CI pipeline pokreće se automatski na svakom `push` i `pull request` prema `master` i `develop` granama putem **GitHub Actions**. Pipeline uključuje:

| Korak | Opis |
|-------|------|
| **Lint provjera** | KTlint i Android Lint analiza koda |
| **Unit testovi** | Pokretanje unit testova s generiranjem JaCoCo izvještaja o pokrivenosti koda |
| **Build Debug APK** | Kompilacija debug verzije aplikacije (nakon uspješnog lint-a i testova) |
| **Build Release APK** | Kompilacija release verzije (samo pri push-u na `master`) |

### 4. Automatsko testiranje

- [x] **JUnit** za unit testove
- [x] **MockK** za mockiranje ovisnosti u testovima
- [x] **Jetpack Compose Testing** (`ui-test-junit4`) za testiranje UI komponenti
- [x] **Espresso** za instrumentacijske (androidTest) testove
- [x] **Coroutines Test** (`kotlinx-coroutines-test`) za testiranje asinkronog koda
- [x] **Navigation Testing** za testiranje navigacijskih tokova
- [x] Automatsko pokretanje testova u CI pipeline-u na svakom push-u i PR-u

### 5. Kontinuirana isporuka (CD)

> **U planiranju** — CD pipeline još nije implementiran.

Planirane aktivnosti za kontinuiranu isporuku:
- [ ] Automatsko potpisivanje release APK-a (keystore putem GitHub Secrets)
- [ ] Distribucija putem Firebase App Distribution za interni QA
- [ ] Automatski deployment na Google Play (Internal Testing track)
- [ ] Verzioniranje buildova na temelju Git tagova

### 6. Analiza kvalitete programskog koda

- [x] **KTlint** — statička analiza i provjera stila Kotlin koda prema službenim konvencijama
- [x] **Android Lint** — detekcija potencijalnih bugova, sigurnosnih propusta i performansnih problema
- [x] **JaCoCo** — generiranje izvještaja o pokrivenosti koda testovima (XML + HTML)
- [x] **SonarCloud** — kontinuirana inspekcija kvalitete koda (statička analiza, code smells, bugovi, sigurnosni propusti) s integracijom JaCoCo izvještaja za prikaz pokrivenosti koda
- [x] Upload lint i JaCoCo izvještaja kao CI artefakata za pregled nakon svakog builda

### 7. Upravljanje konfiguracijom

- [x] **GitHub Secrets** za sigurno pohranjivanje osjetljivih podataka (`GOOGLE_SERVICES_JSON`)
- [x] **Gradle Kotlin DSL** (`build.gradle.kts`) za deklarativnu konfiguraciju projekta i ovisnosti
- [x] **Version Catalog** (`libs.versions.toml`) za centralizirano upravljanje verzijama biblioteka
- [x] `.gitignore` za isključivanje osjetljivih i generiranih datoteka iz repozitorija

### 8. Nadgledanje (Operate & Monitor)

#### Notifikacije i komunikacija

- [x] **GitHub-Slack integracija** — automatske obavijesti u Slack kanalu o push eventima, pull requestovima, code reviewovima i statusu CI pipeline-a

#### Praćenje performansi

Implementirani alati:
- [x] **Firebase Analytics** — praćenje korisničkih događaja i ponašanja unutar aplikacije
- [x] **Firebase Crashlytics** — automatsko prikupljanje i analiza crash izvještaja

Planirano:
- [ ] Integracija Firebase Performance Monitoring za praćenje vremena pokretanja aplikacije, mrežnih zahtjeva i sporog renderiranja
- [ ] Postavljanje alertova za kritične performansne metrike (ANR rate, crash rate)

---

## Dokumentacija

Kompletan **Project Wiki** dostupan je na Confluence:

**[HonNoMachi Confluence Space](https://25-26-izvanredni-tim.atlassian.net/wiki/spaces/HNMT/overview)**

### Ključne stranice

| Dokument                                                                             | Opis                           |
|--------------------------------------------------------------------------------------|--------------------------------|
| [Project Overview](https://25-26-izvanredni-tim.atlassian.net/wiki/x/AQCb)           | Pregled projekta i ciljevi     |
| [System Architecture](https://25-26-izvanredni-tim.atlassian.net/wiki/x/DYDN)        | Dijagram arhitekture sustava   |
| [Development Setup](https://25-26-izvanredni-tim.atlassian.net/wiki/x/DIDE)          | Upute za postavljanje projekta |
| [Product Backlog](https://25-26-izvanredni-tim.atlassian.net/wiki/x/BQCh)            | Lista svih User Storyja        |
| [Korisnička dokumentacija](https://25-26-izvanredni-tim.atlassian.net/wiki/x/AYD6AQ) | Upute za korištenje aplikacije |
| [UX Design](https://25-26-izvanredni-tim.atlassian.net/wiki/x/F4DJ)                  | Wireframovi i dizajn smjernice |
| [CI/CD i DevOps](docs/CICD_DEVOPS.md)                                                | Pipeline, testiranje, deployment |

### Sprint dokumentacija
- [Sprint 01 Folder](https://25-26-izvanredni-tim.atlassian.net/wiki/spaces/HNMT/folder/10158102)

---

## Tim

Projekt razvija tim studenata **Fakulteta organizacije i informatike (FOI)**, Varaždin.

| Član                | Email                      | Uloga     |
|---------------------|----------------------------|-----------|
| **Ivan Giljević**   | igiljevic@student.foi.hr   | Developer |
| **Denis Kuzminski** | dkuzminsk22@student.foi.hr | Developer |
| **Zlatko Pračić**   | zpracic@student.foi.hr     | Developer |
| **Mislav Žnidarec** | mznidarec@student.foi.hr   | Developer |

**Kontakt tima:** [Confluence - Informacije o timu](https://25-26-izvanredni-tim.atlassian.net/wiki/x/BACg)

---

<div align="center">

**Kolegij:** Analiza i razvoj programa  
**Akademska godina:** 2025/2026  
**Institucija:** Fakultet organizacije i informatike, Varaždin

---

**Status projekta:** Sprint 02 - Završava se | Sprint 03 - U planiranju
**Zadnje ažuriranje:** 13.02.2026.

</div>

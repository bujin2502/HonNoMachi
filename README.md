<div align="center">
  <img src=".github/images/AppLogo_v1.png" alt="HonNoMachi Logo" width="200"/>
</div>

# 📚 HonNoMachi  (本の街 - Grad knjiga)
HonNoMachi je Android aplikacija namijenjena svim ljubiteljima knjiga. Aplikacija omogućuje registriranim korisnicima da postanu dio zajednice u kojoj mogu prodavati knjige koje im više ne trebaju i otkrivati nove naslove za svoju kolekciju. Ključne funkcionalnosti uključuju pretraživanje i filtriranje ponude, kreiranje oglasa te kupovinu i prodaju knjiga.

---

## 🔧 Tech Stack
- **Jezik:** Kotlin
- **UI Framework:** Jetpack Compose + Material Design
- **Backend:** Firebase (Authentication, Firestore, Cloud Functions)
- **Plaćanje:** Stripe Android SDK (simulacija)
- **Version Control:** Git / GitHub
- **Project Management:** Jira + Confluence

---

## 📥 Instalacija

Detaljne upute za instalaciju i konfiguraciju projekta dostupne su na Confluence:

👉 **[Development Setup](https://25-26-izvanredni-tim.atlassian.net/wiki/spaces/HNMT/pages/12877836/Development+Setup)**

---

## 📱 Funkcionalnosti

### ✅ Implementirano (Sprint 01)

**Autentifikacija korisnika:**
- Registracija putem email adrese i lozinke
- Email verifikacija kroz Firebase Authentication
- Prijava registriranih korisnika
- Validacija podataka (email format, politika lozinke)
- Ponovno slanje verifikacijskog emaila

### 🚧 Planirano

**Autentifikacija korisnika:**
- Google OAuth prijava (Gmail račun)

**Upravljanje knjigama:**
- Pregled svih dostupnih knjiga
- Pretraga i filtriranje knjiga (naziv, žanr, autor)
- Detaljni pregled pojedine knjige

**Kupovina knjiga:**
- Dodavanje knjiga u košaricu
- Simulacija plaćanja putem Stripe integracije
- Portfelj kupljenih knjiga

**Prodaja knjiga:**
- Kreiranje nove ponude knjige za prodaju
- Upravljanje vlastitim ponudama

**Korisnički profil:**
- Ažuriranje korisničkih podataka (ime, lozinka, kontakt, adresa)

**Administrator panel:**
- Pregled i upravljanje korisnicima
- Suspenzija/reaktivacija korisničkih računa
- Pregled i upravljanje svim knjigama u ponudi

---

## 🐛 Poznati problemi

**Sprint 01:**
- Landscape mode nije podržan na auth stranicama
- Validacija imena dopušta numeričke i specijalne znakove
- Poruke grešaka prikazane na engleskom jeziku (potrebna HR lokalizacija)
- Toast poruke su preduge u portrait modu

**Detaljni QA izvještaji:** [Confluence - QA Dokumentacija](https://25-26-izvanredni-tim.atlassian.net/browse/HNM-28?focusedCommentId=10000) [Trenutno vodi samo na QA za registraciju]

---

## 🤝 Contributing

Projekt koristi **Git Flow** workflow sa sljedećom strukturom:

![GitFlow_example_diagram.png](.github/images/GitFlow_example_diagram.png)

---

## 📚 Dokumentacija

Kompletan **Project Wiki** dostupan je na Confluence:

👉 **[HonNoMachi Confluence Space](https://25-26-izvanredni-tim.atlassian.net/wiki/spaces/HNMT/overview?homepageId=4325618)**

**Ključne stranice:**
- [Project Overview](https://25-26-izvanredni-tim.atlassian.net/wiki/x/AQCb)
- [System Architecture](https://25-26-izvanredni-tim.atlassian.net/wiki/x/DYDN)
- [Development Setup](https://25-26-izvanredni-tim.atlassian.net/wiki/x/DIDE)
- [Sprint 01 Documentation](https://25-26-izvanredni-tim.atlassian.net/wiki/spaces/HNMT/folder/10158102?atlOrigin=eyJpIjoiNmQ0NmU0NDYzMDk0NDA1YWEzNDc0MDM2OGMwOWM5YTUiLCJwIjoiYyJ9)
- [User Guide - Registracija]() [trenutno nigdje ne vodi...]

---

## 👥 Tim

Projekt razvija tim studenata

**Više o timu:** [Confluence - Informacije o timu](https://25-26-izvanredni-tim.atlassian.net/wiki/x/BACg)

---

**Status projekta:** 🟢 Sprint 01 - U tijeku  
**Zadnje ažuriranje:** 23.11 2025
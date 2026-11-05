# Rodinný rozpočet – Android aplikácia (Lokálna & Bezpečná)

Kompletná natívna Android aplikácia v Jetpack Compose pre správu rodinného rozpočtu, 12-mesačného výhľadu hotovosti a správy kreditných kariet. Aplikácia je navrhnutá s dôrazom na **100 % súkromie, okamžitú funkčnosť bez registrácie a bez nutnosti prihlasovania do Google účtu**, s jednoduchou možnosťou zálohovania na Google Disk a priamym zobrazením stavu kariet v hlavnom Prehľade.

---

## Prehľad revidovaných rozhodnutí

> [!IMPORTANT]
> Na základe vašej spätnej väzby sme upravili architektúru aplikácie:

- **100 % funkčnosť bez Google účtu (Maximálne súkromie):** Žiadne povinné prihlasovanie, žiadne prepájanie s vaším osobným Google kontom, žiadne odosielanie finančných údajov na vzdialené servery. Všetky dáta sú uložené bezpečne priamo v pamäti telefónu v lokálnej databáze Room (SQLite).
- **Záloha a synchronizácia cez Google Disk (Export / Import):** Dáta môžete kedykoľvek jedným klikom exportovať do prehľadného záložného súboru (`.json`) a uložiť ho priamo do vášho Google Disku, poslať partnerovi cez WhatsApp/email, alebo spätne importovať na ktoromkoľvek zariadení. Zachovaná je aj spätná kompatibilita s vašou pôvodnou HTML aplikáciou.
- **Kreditné karty priamo v Prehľade:** Do hlavnej obrazovky *Prehľad* dopĺňame kartu so súhrnom kreditných kariet (celkový dlh na kartách, voľný limit, čistý zostatok rodiny a rýchly rozpis kariet s termínmi splatnosti).

---

## 1. Koncept a funkcionalita aplikácie

1. **Prehľad s integrovaným stavom kreditných kariet a 12-mesačným výhľadom:**
   - **Hotovostný zostatok:** Zostatok na bežných účtoch a v hotovosti na začiatku mesiaca.
   - **Karta kreditných kariet v Prehľade:**
     - Celkový dlh na všetkých kreditných kartách spolu.
     - Celkový voľný úverový limit a percentuálne vyčerpanie.
     - **Čistý finančný stav rodiny:** Hotovosť mínus dlh na kartách.
     - Prehľad jednotlivých kariet s dátumom najbližšej splatnosti a čiastkou na úhradu.
   - **12-mesačný výhľad peňažných tokov:**
     - Tabuľka a graf vývoja zostatku na 12 mesiacov dopredu (príjmy, výdavky, mesačné saldo, konečný zostatok).
     - Výstražný banner, ak v ktoromkoľvek mesiaci hrozí deficit (zostatok pod nulou).
   - **Denný harmonogram ("Čo a kedy"):** Detail vybraného mesiaca – chronologický rozpis plánovaných platieb s priebežným zostatkom.

2. **Pravidelné a plánované položky:**
   - Evidencia opakovaných výdavkov a príjmov.
   - Frekvencie: týždenne, mesačne, štvrťročne, ročne a jednorazovo.
   - Kategórie: Bývanie, Energie a internet, Potraviny, Doprava, Deti a škola, Zdravie, Poistky, Splátky úverov, Zábava a voľný čas, Oblečenie, Rezerva a sporenie, Iné / Mzda, Prídavky a dávky, Iný príjem.
   - Matematický engine pre presný výpočet dní platieb (zohľadňujúci rôznu dĺžku mesiacov a týždenné cykly).

3. **Skutočnosť a porovnanie "Plán vs. Skutočnosť":**
   - Pohodlný a rýchly mobilný záznam reálnej platby (názov, suma, kategória, dátum, voľba karty).
   - Výber mesiaca na sledovanie.
   - Analýza **Plán vs. Skutočnosť** podľa kategórií: okamžité porovnanie rozpočtovanej a skutočnej sumy s farebnou indikáciou prekročenia alebo úspory.
   - Zoznam skutočných platieb za vybraný mesiac.

4. **Detailná správa kreditných kariet:**
   - Evidencia kariet: názov, úverový limit, deň výpisu (1–31), deň splatnosti (1–31), počiatočný dlh.
   - Vizuálny progress bar čerpania limitu (upozornenie pri čerpaní nad 80 %).
   - Výpočet sumy z posledného výpisu, termínu splatnosti a nových nákupov.
   - Zaevidovanie splátky a tlačidlo "Pridať do plánu" (prenesie splátku karty do plánovaného cashflow na daný mesiac).

5. **Záloha a prenos dát (Google Disk & JSON):**
   - **Export:** Vytvorenie záložného súboru a odoslanie cez systémový dialóg Androidu (Google Disk, Email, Messenger, interné úložisko).
   - **Import:** Možnosť vybrať súbor z Google Disku alebo úložiska telefónu a načítať všetky položky, platby a karty.
   - **Kompatibilita:** Plná podpora formátu z vašej pôvodnej HTML aplikácie, takže si môžete svoje doterajšie dáta rovno nahrať.

---

## 2. Používateľské rozhranie a vizuálny štýl

- **Aesthetic Direction:** Moderný, čistý Material 3 dizajn s príjemnou smaragdovo-zelenou farbou (`#1F6F5C`), jemnými neutrálnymi kartami a jasným farebným odlíšením príjmov (zelená) a výdavkov (terakota/červená).
- **Spodná navigácia (Navigation Bar):**
  1. `Prehľad` (Cashflow výhľad + stav kariet + harmonogram)
  2. `Pravidelné` (Plánované položky)
  3. `Skutočnosť` (Zápis platieb + Plán vs. Skutočnosť)
  4. `Karty` (Detailná správa kreditiek a splátok)
  5. `Záloha & Nastavenia` (Zálohovanie na Google Disk, import, vymazanie dát)
- **Rýchla akcia (FAB):** Plávajúce tlačidlo pre okamžité pridanie platby z ktorejkoľvek obrazovky.
- **Responzívnosť:** Plynulé zobrazenie na telefónoch aj tabletoch s adaptívnym rozložením.

---

## 3. Technická architektúra a dátový model

```
┌──────────────────────────────────────────────────────────────┐
│                      Jetpack Compose UI                      │
│   OverviewScreen  ·  PlanScreen  ·  ActualScreen  ·  Cards   │
└──────────────────────────────┬───────────────────────────────┘
                               │ StateFlow / Events
┌──────────────────────────────▼───────────────────────────────┐
│                       BudgetViewModel                        │
│   · Cashflow Forecast Engine (12 mesiacov)                   │
│   · Plan vs. Actual porovnanie · Kalkulácie kariet           │
│   · Export a Import zálohy pre Google Disk                   │
└──────────────────────────────┬───────────────────────────────┘
                               │ DAO volania / Flow
┌──────────────────────────────▼───────────────────────────────┐
│                   Room Database (AppDatabase)                │
│   Lokálne SQLite úložisko priamo v telefóne                  │
│   ├── ConfigEntity (počiatočný zostatok)                     │
│   ├── BudgetItemEntity (plánované položky)                   │
│   ├── TransactionEntity (skutočné platby)                    │
│   ├── CreditCardEntity (kreditné karty)                      │
│   └── CardPaymentEntity (splátky kariet)                     │
└──────────────────────────────────────────────────────────────┘
```

---

## 4. Fázy realizácie

1. **Konfigurácia projektu a identita:**
   - Nastavenie unikátneho `applicationId`, názvu aplikácie "Rodinný rozpočet" a vytvorenie vlastnej adaptívnej ikony aplikácie.
2. **Lokálna databázová vrstva (Room & KSP):**
   - Vytvorenie entít: `BudgetItemEntity`, `TransactionEntity`, `CreditCardEntity`, `CardPaymentEntity`, `ConfigEntity`.
   - Vytvorenie DAO rozhraní s reaktívnymi `Flow` dopytmi a `AppDatabase`.
   - Vytvorenie `BudgetRepository` pre zapúzdrenie práce s dátami.
3. **Výpočtová logika rozpočtu a kariet:**
   - Implementácia algoritmu výskytov platieb (`occ` a `plan`) podľa frekvencií (týždenne, mesačne, štvrťročne, ročne, jednorazovo).
   - Generovanie 12-mesačného výhľadu hotovosti a kontrola záporného zostatku.
   - Matematika kreditných kariet: výpočet dlhu, limitov, dátumov výpisov a splatnosti.
4. **Používateľské rozhranie (Compose UI):**
   - Obrazovka **Prehľad**: úvodný zostatok, **karta stavu kreditných kariet**, 12-mesačná tabuľka, detail vybraného mesiaca ("Čo a kedy").
   - Obrazovka **Pravidelné**: formulár novej položky, zoznam plánovaných položiek s možnosťou mazania.
   - Obrazovka **Skutočnosť**: zápis platby, výber mesiaca, tabuľka Plán vs. Skutočnosť, zoznam platieb.
   - Obrazovka **Karty**: karty s ukazovateľom čerpania, záznam splátky, tlačidlo "Pridať do plánu", história splátok, nová karta.
   - Obrazovka **Záloha**: exportovanie zálohy priamo do Google Disku / na zdieľanie, importovanie zálohy (aj z pôvodnej HTML verzie), ukážkové dáta.
5. **Kompilácia a finálne overenie:**
   - Overenie celého zostavenia aplikácie cez `compile_applet`.

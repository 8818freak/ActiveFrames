# Changelog — Active Frames

Alle nennenswerten Änderungen, neueste zuerst. Active Frames ist eine
eigenständige Nachbildung von BlackBerry OS10s „Active Frames": Kacheln der
zuletzt benutzten Apps auf dem Startbildschirm, mit dem Inhalt der letzten
Benachrichtigung und rotem Stern bei Neuem.

## 0.28
- Verbessert: Das Diagnose-Protokoll steht jetzt fest ganz unten, zeigt die
  neuesten Einträge zuerst, und die Schaltflächen (Anzeigen-Schalter,
  „löschen“) stehen darüber. Neuer Schalter „Protokoll anzeigen“ blendet es
  bei Bedarf aus. (Einheitlich in allen Apps.)

## 0.31
- Neu: Unter „Berechtigungen“ lassen sich die gespeicherten Kachelbilder
  (aus Benachrichtigungen und Bildschirmfotos) gezielt löschen – mit
  Sicherheitsabfrage. Die Kacheln bauen sich danach von selbst wieder auf.

## 0.30
- Aufgeräumt: Die Berechtigungen stehen jetzt NUR noch im aufklappbaren
  Abschnitt „Berechtigungen“ – mit Erklärung, wofür jede gebraucht wird, und
  einem Knopf in die passende Systemeinstellung. Die früheren separaten
  Abschnitte (Reihenfolge/Stern/Fotos) entfallen (keine doppelten Einstellungen).
- Behoben: Nach einem System-Update / „Cache leeren“ / SD Maid waren die
  Kachelbilder weg. Sie liegen jetzt im persistenten App-Speicher (nicht mehr
  im Cache) und überleben das. Vorhandene Bilder werden einmalig übernommen.
- Neu: Erinnerung, wenn eine einmal erteilte Berechtigung fehlt (z. B. die
  Bedienungshilfe für die Kachel-Fotos, die Android bei Updates gern entzieht).
  Die Meldung führt direkt zum Wiedererteilen und lässt sich „Ignorieren“
  (falls gewollt). Gemeinsame Funktion für alle Apps.
- Neu: Aufklappbarer Abschnitt „Berechtigungen“ in den Einstellungen (Dreieck
  ▸/▾) – zeigt alle Berechtigungen mit Status; ein Tipp führt je Berechtigung
  in die passende Systemeinstellung.
- Intern: Die Bild-Erkennung aus Benachrichtigungen kommt jetzt aus der
  gemeinsamen Bibliothek (deckt auch das neuere Bild-Format ab Android 12 ab).

## 0.25
- Intern: Die Einstellungs-Sicherung nutzt jetzt die gemeinsame Bibliothek
  „herbers-android-common" (Git-Submodul, Klasse de.herbers.common.SettingsBackup)
  statt einer eigenen Kopie – dieselbe, gepflegte Logik wie in EdgeTab und
  Sucher. Das Sicherungsformat bleibt unverändert. Keine sichtbare Änderung.

## 0.24
- Scroll-Modus auf dem BlackBerry Launcher gründlich repariert (auf dem Gerät
  getestet). Bisher recycelte der BB Launcher die Sammlungs-Kacheln fehlerhaft:
  beim Umsortieren aktualisierte er Bild und Text getrennt, sodass eine Kachel
  z. B. das Bild einer App, aber Symbol/Name einer anderen zeigte – und die
  Reihenfolge stand nicht. Mehrere Bausteine zusammen lösen das:
  - Jede Kachel wird jetzt als EIN Bild gezeichnet (Kachelbild + Symbol + Name +
    Stern zusammen). Bild und Name können damit nicht mehr auseinanderlaufen;
    nur das feste ✕ bleibt ein eigenes, unveränderliches Element.
  - Die Sammlung wird bei jedem vollen Neuaufbau frisch erzeugt (eindeutige
    Adapter-Adresse) und ohne stabile IDs, damit der Launcher keine veralteten
    Kachel-Ansichten wiederverwendet.
  - Die gerade geöffnete App wird sofort vorne einsortiert (unabhängig von der
    Verzögerung des UsageStatsManager); der Start-Launcher selbst wird dabei
    NICHT als „geöffnete App“ gewertet (sonst bekam der Startbildschirm beim
    Zurückkehren eine eigene Kachel und verschob die Reihenfolge).
  - Öffnen aus dem Launcher (Bedienungshilfe) erzwingt nicht mehr bei jedem
    Fenster-Ereignis einen vollen Neuaufbau (das machte die Sammlung „kalt“ und
    Kacheln unanklickbar, 0.21-Fehler): sofort nur schonend, im Hybrid-Modus
    zusätzlich EIN gebündelter, kurz verzögerter voller Neuaufbau pro echtem
    App-Wechsel (unsichtbar, weil die geöffnete App den Startbildschirm verdeckt).
  Ergebnis: Scrollen bleibt flüssig, die Reihenfolge stimmt beim Öffnen (per
  Kachel UND aus dem Launcher), Kacheln passen zusammen und sind anklickbar.

## 0.23
- Rückschritt aus 0.21 behoben: Die Bedienungshilfe machte seit 0.21 bei JEDEM
  App-Wechsel einen vollen Neuaufbau (setRemoteAdapter). Das überforderte im
  Scroll-Modus den BlackBerry Launcher – die Sammlung wurde „kalt", Kacheln
  waren teils nicht mehr anklickbar, und die Reihenfolge blieb hängen. Die
  Bedienungshilfe frischt jetzt wieder nur schonend auf; den vollen,
  umsortierenden Weg nehmen ausschließlich die seltenen, ausdrücklichen
  Aktionen (Kacheldruck, Schließen). Die „gerade geöffnet"-Vormerkung aus 0.22
  bleibt, damit die zuletzt geöffnete App beim nächsten vollen Neuaufbau sofort
  oben links steht. Hinweis: Zuverlässiges Umsortieren beim Öffnen AUS dem
  Launcher heraus ist im Scroll-Modus auf dem BlackBerry Launcher weiterhin
  nicht sicher machbar – dafür das feste Raster (Scrollen aus) oder „Widget 2"
  in EdgeTab nutzen.

## 0.22
- Sortierung nach Öffnungsreihenfolge korrigiert: Die gerade geöffnete App
  (per Kachel, aus dem Launcher oder aus der EdgeTab-Karte) steht jetzt sofort
  oben links – unabhängig davon, wie schnell der UsageStatsManager das
  In-den-Vordergrund-Kommen verbucht. Der hinkt ein paar Sekunden nach; im
  schonenden/Hybrid-Modus, wo der volle (einzig umsortierende) Neuaufbau genau
  beim Öffnen läuft, rendert der Neuaufbau sonst noch die alte Reihenfolge und
  die bleibt hängen (Ursache dafür, dass die Kacheln zuletzt nicht mehr korrekt
  nach Öffnungsreihenfolge sortiert waren). Die gerade geöffnete App wird jetzt
  für kurze Zeit vorrangig vorne einsortiert, danach übernimmt wieder der
  UsageStatsManager (der bis dahin nachgezogen hat).

## 0.21
- Hybrid-Modus deckt jetzt auch das Öffnen ab: Der Zusatzschalter heißt „Bei
  Aktionen voll auffrischen (Öffnen/Schließen)“ und lässt neben dem Schließen
  auch das Öffnen einer Kachel bzw. das Öffnen einer App aus dem Launcher den
  vollen, zuverlässigen Weg nehmen. So übernimmt der BlackBerry Launcher beim
  Öffnen wieder die neue Kachel-Reihenfolge (zuvor blieb sie im schonenden
  Modus stehen). Beim Öffnen ist der volle Weg unsichtbar, weil die geöffnete
  App den Startbildschirm gerade verdeckt. Scrollen, eintreffende
  Benachrichtigungen und das Nachschärfen der Bilder bleiben schonend (flüssig).

## 0.20
- Dritter Auffrisch-Modus (Hybrid) als Zusatzschalter „Beim Schließen voll
  auffrischen“ unter „Schonend auffrischen“: Öffnen/Scrollen/Benachrichtigungen
  bleiben flüssig (schonend), nur das ✕ nimmt den vollen, zuverlässigen Weg.
  Das ist auf Launchern, die das schonende Auffrischen nur unzuverlässig annehmen
  (BlackBerry Launcher), ein zuverlässiges Schließen – und weckt die Sammlung
  wieder auf, sodass das schonende Auffrischen danach wieder greift. So gibt es
  jetzt drei Wege: voll (immer zuverlässig, ruckelt), schonend (flüssig, auf
  manchen Launchern unzuverlässig) und hybrid (flüssig, Schließen zuverlässig).

## 0.19
- Neue Option „Schonend auffrischen (moderne Launcher)“ (Einstellungen, nur im
  Scroll-Modus): aktualisiert die Kacheln über notifyAppWidgetViewDataChanged,
  ohne die Sammlung neu aufzubauen – auf modernen Launchern verschwindet damit
  das Bild-„Springen“ beim Schließen. Standard AUS, mit Warnhinweis: Manche
  Launcher (u. a. der BlackBerry Launcher) ignorieren den Weg – dann lässt sich
  keine Kachel mehr schließen; dann wieder ausschalten.

## 0.18
- Springen im Scroll-Modus abgemildert: Die verkleinerten Kachelbilder (und
  Logos) liegen jetzt in einem Speicher-Cache. Beim Neuaufbau der Sammlung nach
  dem Schließen bindet der Launcher die Kacheln dadurch deutlich schneller wieder
  (kein erneutes Dekodieren von der Platte je Kachel) – das Springen wird kürzer
  und unauffälliger. Ganz vermeiden lässt es sich auf dem BlackBerry Launcher
  nicht (er baut Sammlungen beim Auffrischen neu auf); ruckelfrei bleiben der
  feste Modus und EdgeTabs „Widget 2“.

## 0.17
- Rücknahme der 0.16-Auffrischungsänderung: Kacheln ließen sich damit nicht mehr
  per ✕ schließen. Grund: Der BlackBerry Launcher aktualisiert eine Sammlung nur
  über den vollständigen updateAppWidget-Weg, nicht über das schonende
  notifyAppWidgetViewDataChanged. Das Schließen (und das Aktualisieren von
  Stern/Bild) funktioniert damit wieder. Kehrseite im Scroll-Modus: der Launcher
  baut die Sammlung dabei neu auf (kurzes Bild-„Springen“) – eine Grenze dieses
  Launchers. Ruckelfrei bleiben der feste Modus und EdgeTabs „Widget 2“.
- Die höhere Bildauflösung (400 px) aus 0.16 bleibt erhalten.

## 0.16
- Bilder-Springen beim Schließen im SCROLL-Modus behoben: Beim Auffrischen wird
  die Sammlung jetzt nur noch benachrichtigt (notifyAppWidgetViewDataChanged)
  statt bei jeder Änderung den RemoteAdapter neu zu setzen. Vorher baute der
  Launcher die ganze Sammlung neu auf, wodurch die Kachelbilder mehrfach an
  verschobene Kacheln sprangen. (0.15 hatte nur die feste Darstellung betroffen.)
- Gespeicherte Kachelbilder mit höherer Auflösung (400 statt 260 px längste
  Kante) – schärfere große Kacheln, vor allem in EdgeTabs „Widget 2“. Das
  Home-Widget skaliert daraus je nach Kachelzahl passend herunter.

## 0.15
- Kein „Springen“ der Kachelbilder mehr beim Schließen (feste Darstellung): Die
  Bilder werden jetzt als kleine Bitmaps direkt eingebettet statt per
  content://-URI übertragen. Vorher lud der Launcher beim Neuaufbau des Widgets
  jede URI einzeln asynchron nach, wodurch Bilder kurz (bis zu mehrmals) an die
  falsche Kachel sprangen – genau darum springt BlackBerrys Hub-Widget nicht.
  Die Bildgröße ist an die Kachelzahl gekoppelt, damit die Übertragung klein
  bleibt.

## 0.14
- Der rote Stern erlischt jetzt auch, wenn die zugehörige Benachrichtigung aus
  dem System entfernt (weggewischt/gelöscht) wird – nicht mehr nur beim Öffnen
  der App. Er bleibt, solange noch eine echte Benachrichtigung dieser App aktiv
  ist.
- Das ✕ zum Schließen einer Kachel hat jetzt ein deutlich größeres Tippziel
  (füllt die Balkenhöhe). Vorher ließ sich der kleine Knopf leicht verfehlen –
  dann geschah gar nichts. Hinweis: Beim Schließen baut der Launcher das Widget
  neu auf und lädt dabei die Kachelbilder kurz neu (sie können einen Moment an
  der falschen Kachel erscheinen) – eine Grenze der Home-Widgets (RemoteViews).
  Ruckelfrei und verlässlich ist das native „Widget 2“ in EdgeTab.

## 0.13
- Sichern/Wiederherstellen erzeugt bzw. liest jetzt eine echte Datei über den
  System-Dateidialog (Storage Access Framework), statt nur Text anzuzeigen –
  wie bei EdgeTab, ohne Speicher-Berechtigung.

## 0.12
- Scroll-Modus des Home-Screen-Widgets (Widget 1) auf älteren Launchern
  repariert: Die Kachelbilder werden jetzt als kleine Bitmaps direkt
  übertragen (setImageViewBitmap) statt per content://-URI. Auf dem
  BlackBerry Launcher lud die Sammlung URIs asynchron und band sie beim
  Wiederverwenden von Kacheln an die falsche Zeile - daher das Vertauschen
  von Bild und Name. BlackBerrys eigenes, sauber scrollendes Hub-Widget macht
  es genauso (kleine Bitmaps pro Zeile). In einer Sammlung wird ohnehin nur
  das Sichtbare übertragen, das sprengt den Binder-Speicher nicht.
- Apps, die Bildschirmfotos unterbinden (z. B. BBMe, Banking), lieferten mit
  der Bedienungshilfe ein schwarzes Kachelbild. Jetzt wird ein solches
  Schwarzbild erkannt und stattdessen das App-Logo kachelgroß (mittig,
  unverzerrt, unbeschnitten) auf dunklem Grund gezeigt.
- Solange (noch) kein Kachelbild vorliegt, zeigt die Kachel jetzt ebenfalls das
  App-Logo kachelgroß (mittig, unverzerrt) statt nur des App-Namens – gilt für
  beide Darstellungen (fest und scrollbar).
- Neu: Sichern &amp; Wiederherstellen der Einstellungen als Text zum Kopieren
  (Startbildschirm der App), analog zu EdgeTab.

## 0.11
- Datenkanal für „Widget 2": Ein neuer, streng abgesicherter Anbieter
  (FramesDataProvider) stellt die Kachel-Reihenfolge, die Sterne und die
  Bilder einer anderen App des Nutzers bereit - gedacht für die native
  „Aktive Kacheln"-Karte in EdgeTab. So bleibt Active Frames die einzige
  Datenquelle (mit Nutzungsdaten-/Benachrichtigungs-/Bedienungshilfe-Zugriff);
  EdgeTab braucht dafür keine eigenen heiklen Berechtigungen und bekommt die
  echten App-Fotos „geschenkt". Zugriff ausschließlich für EdgeTab (per
  Paketnamen geprüft), kein fremder Zugriff möglich.

## 0.10
- Das Home-Screen-Widget heißt jetzt „Widget 1" (eine native, scrollbare
  Active-Frames-Karte für EdgeTab als „Widget 2" folgt).
- Neuer Scroll-Schalter (Einstellungen): AN = scrollbar (mehr Kacheln als
  sichtbar), AUS = die feste, immer korrekte Darstellung mit variablen
  Reihenhöhen. Mit Warnhinweis: Auf älteren Launchern (z. B. BlackBerry
  Launcher) kann die scrollbare Variante Bild und Name vertauschen - dann
  einfach ausschalten.

## 0.9
- Falscher „Neues"-Stern behoben (trat u.a. beim Hub-Posteingang ohne Grund
  auf): Der Stern erscheint jetzt nur noch bei einer echten, wegwischbaren
  Einzel-Benachrichtigung - nicht mehr bei Gruppen-Sammelmeldungen oder
  Dienst-/Transport-/Fortschritts-/System-Hinweisen (Sync, Mediensteuerung ...).

## 0.8
- Vertauschen von Bild/Symbol/Name endgültig behoben: Das Widget wird nicht
  mehr über eine RemoteViews-Sammlung (Adapter) aufgebaut - manche (ältere)
  Launcher recyceln deren Kacheln fehlerhaft und wenden beim Wiederverwenden
  nur einen Teil der Änderungen an. Jetzt wird die ganze Ansicht als eine
  RemoteViews mit selbst gebauten Zeilen/Kacheln erzeugt, ohne Recycling.
- Variable Reihenhöhen (wie BlackBerry OS10): die oberen Reihen groß, danach
  kleiner. Einstellbar: Anzahl der großen Reihen und Höhe der kleineren in %.
- Hinweis: Ohne Sammlung entfällt das Scrollen - sichtbar ist, was in die
  Widget-Größe passt (Widget einfach größer ziehen für mehr Kacheln).

## 0.7
- Behebt, dass Bild, App-Symbol und Name einer Kachel nicht zusammenpassten
  und die Reihenfolge nicht aktualisierte: Die großen Kachelbilder wurden als
  Bitmaps ins Widget geschickt und sprengten bei mehreren Kacheln die
  Übertragungsgrenze (Binder ~1 MB), wodurch Teile vertauscht/verworfen wurden.
  Bilder werden jetzt per content-URI über einen internen Bild-Provider geladen
  (der Launcher holt sie selbst), sodass nur noch winzige Daten je Kachel
  übertragen werden.

## 0.6
- Widget-Hintergrund transparent – das Bildschirm-Hintergrundbild ist zwischen
  den Kacheln zu sehen.
- Kachelhöhe: neuer Automatik-Modus, der die Kacheln ans Bildschirmformat
  anpasst (aus Widget-Breite, Spaltenzahl und Bildschirm-Seitenverhältnis);
  passt sich beim Skalieren des Widgets mit an. Weiterhin auch manuell
  einstellbar (wenn die Automatik aus ist).
- ✕ auf der Kachel entfernt jetzt wirklich die Kachel (die anderen rücken nach).
  Ein echtes Beenden der App gibt Android für Fremd-Apps nicht her – daher
  „nur" ausblenden; die Kachel kommt zurück, sobald die App wieder benutzt wird
  oder etwas Neues meldet.
- Englisch als zweite Sprache (folgt automatisch der Gerätesprache).

## 0.5
- Änderungsprotokoll in der App: unter „Änderungsprotokoll" auf dem
  Startbildschirm ein- und ausblendbar (liest diese Datei).

## 0.4
- Fehler behoben, bei dem Bild/Symbol nicht zu Name/Start der Kachel passten
  (Kacheln waren vertauscht): Die Kachel-Liste wird jetzt atomar aufgebaut und
  jede Kachel hat eine stabile Kennung – Bild, Symbol, Name und Tippen gehören
  wieder zusammen.
- Stern jetzt mit fünf Armen (statt sechs).
- Mehr Abstand zwischen den Kacheln, damit der Stern in der Ecke Platz hat und
  nur seine eigene Kachel zu etwa einem Viertel überlappt.
- Bedienungshilfe fotografiert/aktualisiert nur noch bei echtem App-Wechsel
  (ruhiger, weniger Flackern).

## 0.3
- Untere Leiste je Kachel wie im BlackBerry-Vorbild: links das App-Symbol,
  daneben der Name, rechts ein ✕ zum Schließen der Kachel (kommt zurück, sobald
  die App wieder benutzt wird oder etwas Neues meldet).
- Roter „Neues"-Stern jetzt als weißer BB-Asterisk auf rotem Kreis, oben rechts
  über die Ecke ragend.
- MRU-Fehler behoben: Die Kachelreihenfolge aktualisiert sich jetzt laufend
  (bei jedem App-Wechsel, sofern die Bedienungshilfe an ist, sowie nach dem
  Entsperren) – die zuletzt benutzte App steht oben links. Der Startbildschirm-
  Launcher wird nicht mehr als „App" mitgezählt.

## 0.2
- Echte App-Fotos auf den Kacheln (optional): über einen Bedienungshilfe-
  Dienst macht Active Frames beim Öffnen einer App ein Bildschirmfoto ihres
  Zustands (`takeScreenshot`, ab Android 11) – die einzige Möglichkeit für
  eine Nicht-System-App, echte Active-Frames-Optik zu zeigen, ohne Root und
  ohne MediaProjection-Warnsymbol. Nur aktiv, wenn ausdrücklich eingeschaltet;
  Fotos bleiben app-intern auf dem Gerät, sichere Apps (Banking) bleiben schwarz.
- Schnellstart-Kacheln: Apps zum Anpinnen wählbar (stehen immer vorne).
- Kacheln jetzt im Hochkant-/Bildschirmformat statt quadratisch; Standard
  2 Spalten (weiterhin frei einstellbar, z. B. 4 wie auf dem Passport).
- Frame-Optik: ohne Bild zeigt die Kachel den großen App-Namen (wie BBs leere
  Frames) statt nur eines zentrierten Symbols.

## 0.1
- Erste Version. Scrollbares Raster-Widget (frei skalierbar), Kacheln der
  zuletzt benutzten Apps in MRU-Reihenfolge (oben links zuletzt geöffnet,
  wie BlackBerry).
- Einstellbar: Spaltenzahl, Kachelhöhe (= Seitenverhältnis, auch rechteckig
  statt nur quadratisch), Anzahl der Kacheln (überzählige per Hochwischen),
  App-Namen ein/aus.
- Roter Stern bei etwas Neuem und Kachelbild aus der letzten Benachrichtigung
  (Foto/Cover/großes Symbol), sonst das App-Symbol — der einzige für eine
  Nicht-System-App erreichbare Weg zu „lebendigen" Kacheln (echte Screenshots
  fremder Apps sind auf Android gesperrt).
- Tippen startet die App und löscht deren Stern.
- Kein Werbe-/Analytics-/Billing-Ballast, keine Internet-Berechtigung.

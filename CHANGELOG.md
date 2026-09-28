# Changelog — Active Frames

Alle nennenswerten Änderungen, neueste zuerst. Active Frames ist eine
eigenständige Nachbildung von BlackBerry OS10s „Active Frames": Kacheln der
zuletzt benutzten Apps auf dem Startbildschirm, mit dem Inhalt der letzten
Benachrichtigung und rotem Stern bei Neuem.

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

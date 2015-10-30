---

![TU Dortmund Logo](http://www.ub.tu-dortmund.de/images/tu-logo.png)

![UB Dortmund Logo](http://www.ub.tu-dortmund.de/images/ub-schriftzug.jpg)

---

# *Middleware* für die UA-Ruhr-Bibliographie

Bezug: [hb_ng](https://github.com/ubbochum/hb_ng)


Die Middleware stellt folgenden Endpoint zur Verfügung:

    http://{host}:{port}/task

Dieser verarbeitet *Multipart-Requests* mit den Parametern:

    file-{lfd. Nr.} : Publikationsliste, z.B. als Word- oder PDF-Datei
    dataset-mods : MODS-Daten
    dataset-csl : CSL-Daten (optional)
    resource-{lfd. Nr.}-type : Document type einer Upload-Datei (z.B. Preprint, Postprint, Datensatz). (optional)
    resource-{lfd. Nr.}-file : Die Datei für das Upload. (optional)
    resource-{lfd. Nr.}-note : Bemerkungen zum Upload. (optional)

Beispiele mit cURL:

    curl -X POST -F resource-0-file=@Beitrag.pdf -F resource-0-type=Preprint -F resource-0-note='Bla Blub Hmpf' -F dataset-mods=@TMP/book-example.mods.xml http://localhost:5220/new?uuid=0123456789-test
    
    curl -X POST -F file-0=@TMP/liste.docx http://localhost:5220/task



# Kontakt

**data@ubdo - Datenplattform der Universitätsbibliothek Dortmund**

Technische Universität Dortmund // Universitätsbibliothek // Bibliotheks-IT // Vogelpothsweg 76 // 44227 Dortmund

[Webseite](https://data.ub.tu-dortmund.de) // [E-Mail](mailto:opendata@ub.tu-dortmund.de)

---

![Creative Commons License](http://i.creativecommons.org/l/by/4.0/88x31.png)

This work is licensed under a [Creative Commons Attribution 4.0 International License (CC BY 4.0)](http://creativecommons.org/licenses/by/4.0/)

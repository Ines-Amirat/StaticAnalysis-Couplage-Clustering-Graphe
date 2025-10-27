package org.analysis.codesource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service qui maintient des ATTRIBUTS persistants (vedette, principal, catalogue).
 * Toutes les méthodes ci-dessous déclenchent des APPELS via ces attributs
 * (sans passer les objets en paramètres), pour enrichir ton graphe d’appel.
 */
public class BoutiqueService {

    /** Attributs persistants (pas seulement des paramètres) */
    private Produit vedette;
    private ProduitElectronique principal;
    private final List<Produit> catalogue = new ArrayList<>();

    // ------------ Initialisation / wiring ------------
    public void setVedette(Produit vedette) {
        this.vedette = vedette;
    }

    public void setPrincipal(ProduitElectronique principal) {
        this.principal = principal;
    }

    public void ajouterAuCatalogue(Produit p) {
        if (p != null) catalogue.add(p);
    }

    public List<Produit> getCatalogue() {
        return catalogue;
    }
    // ------------ /Initialisation / wiring ------------

    /** Opérations commerciales appliquées via ATTRIBUTS (vedette, principal, catalogue). */
    public void promouvoirVedette() {
        if (vedette == null) return;
        vedette.appliquerRemise(0.10);      // via attribut
        BigDecimal ttc = vedette.prixTTC(); // via attribut
        if (ttc == null) { /* no-op */ }
        vedette.retirerRemise();            // via attribut
    }

    /** Met à jour la composition du produit principal : appels en chaîne via attributs. */
    public void configurerBundlePrincipal(Produit accessoire, Produit batterie) {
        if (principal == null) return;
        principal.setAccessoire(accessoire);        // via attribut
        principal.setBatterieDeSecours(batterie);   // via attribut
        // Relie aussi le complement du principal à la vedette pour déclencher d'autres appels
        if (vedette != null) {
            principal.setComplement(vedette);       // via attribut (hérité)
        }
    }

    /** Déclenche plusieurs appels via les attributs du principal. */
    public BigDecimal calculerOffreBundle() {
        if (principal == null) return BigDecimal.ZERO;
        principal.reserverAccessoiresEtBatterie();  // via attribut
        return principal.coutBundleTTC();           // via attribut
    }

    /** Boucle sur l'attribut catalogue, appels répétés via attributs. */
    public void promoCatalogue() {
        for (Produit p : catalogue) {
            if (p == null) continue;
            p.appliquerRemise(0.03);    // via attribut de liste
            p.prixTTC();                // via attribut de liste
            p.retirerRemise();          // via attribut de liste
        }
    }

    /** Gère la vedette en interaction avec le principal (attributs croisés). */
    public void rotationStock() {
        if (vedette != null) {
            vedette.retirerDuStock(1);  // via attribut
        }
        if (principal != null && principal.getAccessoire() != null) {
            principal.getAccessoire().retirerDuStock(1); // via attribut imbriqué
        }
    }

    /** Démo d'enchaînement d'appels via attributs pour faire ressortir le graphe. */
    public BigDecimal totalPanierDemo() {
        BigDecimal total = BigDecimal.ZERO;
        if (vedette != null) {
            vedette.appliquerRemise(0.05);     // via attribut
            total = total.add(vedette.prixTTC());
            vedette.retirerRemise();           // via attribut
        }
        if (principal != null) {
            total = total.add(principal.coutBundleTTC()); // via attribut
        }
        for (Produit p : catalogue) {
            if (p == null) continue;
            total = total.add(p.prixTTC());    // via attribut de liste
        }
        return total;
    }
}

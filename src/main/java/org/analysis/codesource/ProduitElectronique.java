package org.analysis.codesource;

import java.math.BigDecimal;

/**
 * Produit électronique qui possède des attributs de type Produit.
 * Appelle leurs méthodes (via attributs) pour enrichir le graphe d’appels.
 */
public class ProduitElectronique extends Produit {

    /** Attributs pour créer des appels via composition */
    private Produit accessoire;
    private Produit batterieDeSecours;

    public ProduitElectronique(String reference, String nom, BigDecimal prixHT, BigDecimal tva, int stock) {
        super(reference, nom, prixHT, tva, stock);
    }

    public void setAccessoire(Produit accessoire) {
        this.accessoire = accessoire;
    }

    public Produit getAccessoire() {
        return accessoire;
    }

    public void setBatterieDeSecours(Produit batterieDeSecours) {
        this.batterieDeSecours = batterieDeSecours;
    }

    public Produit getBatterieDeSecours() {
        return batterieDeSecours;
    }

    /** Appels multiples via attributs (this, accessoire, batterie, et même le complement hérité de Produit). */
    public BigDecimal coutBundleTTC() {
        BigDecimal total = this.prixTTC();                 // Produit(prixTTC) via héritage
        if (accessoire != null) {
            accessoire.appliquerRemise(0.05);              // via attribut
            total = total.add(accessoire.prixTTC());       // via attribut
            accessoire.retirerRemise();                    // via attribut
        }
        if (batterieDeSecours != null) {
            batterieDeSecours.retirerDuStock(1);           // via attribut
            total = total.add(batterieDeSecours.prixTTC()); // via attribut
        }
        // Appels via l'attribut complement hérité de Produit (si défini)
        Produit complement = this.getComplement();
        if (complement != null) {
            complement.appliquerRemise(0.02);              // via attribut (hérité)
            total = total.add(complement.prixTTC());       // via attribut (hérité)
            complement.retirerRemise();                    // via attribut (hérité)
        }
        return total;
    }

    /** Réserve différents éléments liés via attributs. */
    public void reserverAccessoiresEtBatterie() {
        if (accessoire != null) {
            accessoire.retirerDuStock(1);                  // via attribut
        }
        if (batterieDeSecours != null) {
            batterieDeSecours.retirerDuStock(1);           // via attribut
        }
    }
}

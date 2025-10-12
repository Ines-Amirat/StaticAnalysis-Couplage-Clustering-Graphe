package org.analysis.codesource;

import java.math.BigDecimal;
import java.util.Objects;

/** Orchestrateur très simple qui appelle les autres classes. */
public class BoutiqueService {

    /** Ajoute au “panier” en retirant du stock et renvoie le total TTC. */
    public BigDecimal ajouterAuPanier(ProduitElectronique p, int quantite) {
        Objects.requireNonNull(p);
        if (!p.disponible(quantite)) throw new IllegalStateException("Indisponible");
        p.retirerDuStock(quantite);                 // appelle Produit.retirerDuStock
        BigDecimal total = p.prixTTC().multiply(BigDecimal.valueOf(quantite))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        // Appel d'une méthode de la sous-classe pour affichage/log
        p.log("Ajout panier x" + quantite + " -> total " + total + "€");
        return total;
    }

    /** Applique une remise commerciale à un produit (appelle la sous-classe). */
    public void promouvoir(ProduitElectronique p, double remise) {
        Objects.requireNonNull(p);
        p.appliquerRemise(remise);                  // surchargée: cap à 30% pour l'électronique
    }

    /** Estime un prix TTC après remise temporaire (sans toucher le stock). */
    public BigDecimal estimer(ProduitElectronique p, int quantite, double remiseTemporaire) {
        Objects.requireNonNull(p);
        // on applique une remise "temporaire", puis on la retire (appels croisés)
        p.appliquerRemise(remiseTemporaire);
        BigDecimal total = p.prixTTC().multiply(BigDecimal.valueOf(quantite))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        p.retirerRemise();
        return total;
    }

    /** Active une TVA éco sur un produit électronique (appelle méthode spécifique). */
    public void activerTvaEco(ProduitElectronique p) {
        Objects.requireNonNull(p);
        p.activerTvaEco();                          // appelle setTva(...) défini dans Produit
    }
}

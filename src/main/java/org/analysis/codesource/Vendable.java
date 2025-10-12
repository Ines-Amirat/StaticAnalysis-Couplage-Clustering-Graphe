package org.analysis.codesource;

import java.math.BigDecimal;

/** Contrat minimal d’un article vendable. */
public interface Vendable {
    String code();
    String nom();
    String categorie();

    BigDecimal prixHT();
    BigDecimal tva();     // ex: 0.20 pour 20%
    default BigDecimal prixTTC() {
        return prixHT().multiply(BigDecimal.ONE.add(tva()))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    boolean disponible(int quantite);
    void ajouterAuStock(int quantite);
    void retirerDuStock(int quantite);

    /** Appliquer/retirer une remise (0.0–1.0). */
    void appliquerRemise(double pourcentage);
    void retirerRemise();
}

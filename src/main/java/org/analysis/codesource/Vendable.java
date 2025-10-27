package org.analysis.codesource;

import java.math.BigDecimal;

public interface Vendable {
    BigDecimal getPrixHT();
    BigDecimal prixTTC();
    void appliquerRemise(double pourcentage); // 0.10 = 10%
    void retirerRemise();
    void retirerDuStock(int quantite);
}

package org.analysis.codesource;

import java.math.BigDecimal;

/** Sous-classe : règles simples spécifiques à l'électronique. */
public class ProduitElectronique extends Produit {
    private final int garantieMois;

    public ProduitElectronique(String code, String nom, BigDecimal prixHT, int stock, int garantieMois) {
        super(code, nom, "Électronique", prixHT, new BigDecimal("0.20"), stock);
        this.garantieMois = Math.max(0, garantieMois);
        log("Garantie: " + this.garantieMois + " mois");
    }

    public int garantieMois() { return garantieMois; }

    /** Cap la remise à 30% sur l’électronique, puis appelle la super-méthode. */
    @Override
    public void appliquerRemise(double pct) {
        double limite = Math.min(pct, 0.30);
        super.appliquerRemise(limite);
    }

    /** Exemple : ajuster la TVA à 5.5% pour une promo éco (appelle super). */
    public void activerTvaEco() {
        setTva(new BigDecimal("0.055"));
    }

    /** Petit résumé utilisé par le service (appel croisé). */
    public String resume() {
        return nom() + " [" + code() + "] " + garantieMois + " mois - TTC: " + prixTTC() + "€";
    }
}

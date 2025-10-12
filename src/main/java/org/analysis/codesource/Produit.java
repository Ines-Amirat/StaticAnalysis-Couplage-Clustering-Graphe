package org.analysis.codesource;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Base commune à tous les produits. */
public abstract class Produit implements Vendable {
    private final String code;
    private final String nom;
    private final String categorie;
    private BigDecimal prixHT;
    private BigDecimal tva;           // 0.20 = 20%
    private int stock;
    private double remise = 0.0;      // 0–1
    private final List<String> journal = new ArrayList<>();

    protected Produit(String code, String nom, String categorie,
                      BigDecimal prixHT, BigDecimal tva, int stock) {
        this.code = code;
        this.nom = nom;
        this.categorie = categorie;
        this.prixHT = prixHT;
        this.tva = tva;
        this.stock = Math.max(0, stock);
        log("Création produit " + nom + " (" + categorie + ")");
    }

    @Override public String code() { return code; }
    @Override public String nom() { return nom; }
    @Override public String categorie() { return categorie; }
    @Override public BigDecimal prixHT() { return prixHT.multiply(BigDecimal.valueOf(1.0 - remise)); }
    @Override public BigDecimal tva() { return tva; }

    @Override public boolean disponible(int q) { return q > 0 && stock >= q; }
    public int stock() { return stock; }
    public List<String> journal() { return journal; }

    @Override
    public void ajouterAuStock(int q) {
        if (q <= 0) return;
        stock += q;
        log("Stock +"+q+" (stock="+stock+")");
    }

    @Override
    public void retirerDuStock(int q) {
        if (!disponible(q)) throw new IllegalStateException("Stock insuffisant");
        stock -= q;
        log("Stock -"+q+" (stock="+stock+")");
    }

    @Override
    public void appliquerRemise(double pct) {
        double p = Math.max(0.0, Math.min(1.0, pct));
        this.remise = p;
        log("Remise appliquée: " + (int)(p*100) + "%");
    }

    @Override
    public void retirerRemise() {
        this.remise = 0.0;
        log("Remise retirée");
    }

    /** Méthode utilitaire utilisée/appe­lée par la sous-classe (appel croisé). */
    protected void log(String msg) { journal.add(msg); }

    /** Permet à la sous-classe d'ajuster TVA si besoin. */
    protected void setTva(BigDecimal nouvelleTva) {
        if (nouvelleTva == null || nouvelleTva.signum() < 0) return;
        this.tva = nouvelleTva;
        log("TVA changée -> " + nouvelleTva);
    }

    /** Idem pour prix de base HT (avant remise). */
    protected void setPrixHTBase(BigDecimal nouveauPrix) {
        if (nouveauPrix == null || nouveauPrix.signum() < 0) return;
        this.prixHT = nouveauPrix;
        log("Prix HT de base -> " + nouveauPrix);
    }
}


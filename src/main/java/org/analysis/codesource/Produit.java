package org.analysis.codesource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Produit de base.
 * Contient des attributs qui référencent d'autres Produits pour provoquer des appels transverses
 * via attributs (et non seulement via paramètres de méthodes).
 */
public class Produit implements Vendable {

    private String reference;
    private String nom;
    private BigDecimal prixHT;        // ex: 99.99
    private BigDecimal tva;           // ex: 0.20
    private int stock;
    private BigDecimal remise;        // [0..1]

    /** Attributs qui créent des appels entre classes */
    private Produit complement;               // un produit complémentaire
    private final List<Produit> recommandations = new ArrayList<>(); // suggestions

    public Produit(String reference, String nom, BigDecimal prixHT, BigDecimal tva, int stock) {
        this.reference = Objects.requireNonNull(reference);
        this.nom = Objects.requireNonNull(nom);
        this.prixHT = prixHT == null ? BigDecimal.ZERO : prixHT;
        this.tva = tva == null ? BigDecimal.ZERO : tva;
        this.stock = Math.max(0, stock);
        this.remise = BigDecimal.ZERO;
    }

    // ------------ API Vendable ------------
    @Override
    public BigDecimal getPrixHT() {
        return prixHT;
    }

    @Override
    public BigDecimal prixTTC() {
        // TTC = HT * (1 - remise) * (1 + TVA)
        BigDecimal un = BigDecimal.ONE;
        BigDecimal factorRemise = un.subtract(remise == null ? BigDecimal.ZERO : remise);
        BigDecimal factorTVA = un.add(tva == null ? BigDecimal.ZERO : tva);
        return prixHT
                .multiply(factorRemise)
                .multiply(factorTVA)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void appliquerRemise(double pourcentage) {
        if (pourcentage <= 0) return;
        BigDecimal p = BigDecimal.valueOf(pourcentage);
        if (p.compareTo(BigDecimal.ONE) > 0) {
            p = p.movePointLeft(2); // 10 -> 0.10
        }
        if (remise == null) remise = BigDecimal.ZERO;
        remise = remise.add(p);
        if (remise.compareTo(BigDecimal.ONE) > 0) remise = BigDecimal.ONE;
    }

    @Override
    public void retirerRemise() {
        remise = BigDecimal.ZERO;
    }

    @Override
    public void retirerDuStock(int quantite) {
        if (quantite <= 0) return;
        stock = Math.max(0, stock - quantite);
    }
    // ------------ /API Vendable ------------

    // ------------ Attributs de relation + appels via attributs ------------
    public void setComplement(Produit complement) {
        this.complement = complement;
    }

    public Produit getComplement() {
        return complement;
    }

    public void ajouterRecommandation(Produit p) {
        if (p != null) recommandations.add(p);
    }

    public List<Produit> getRecommandations() {
        return recommandations;
    }

    /** Déclenche plusieurs appels sur l’attribut complement (si présent). */
    public BigDecimal comparerAvecComplement() {
        if (complement == null) return this.prixTTC();
        // appels via attribut
        BigDecimal ttcThis = this.prixTTC();
        complement.appliquerRemise(0.03);
        BigDecimal ttcCompl = complement.prixTTC();
        complement.retirerRemise();
        return ttcThis.min(ttcCompl);
    }

    /** Boucle sur l’attribut recommandations et appelle leurs méthodes. */
    public BigDecimal totalRecommandationsTTC() {
        BigDecimal total = BigDecimal.ZERO;
        for (Produit p : recommandations) {
            if (p == null) continue;
            p.appliquerRemise(0.01);        // appel via attribut
            total = total.add(p.prixTTC()); // appel via attribut
            p.retirerRemise();              // appel via attribut
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** Crée un mini-pack avec le complement, manipule le stock du complement. */
    public void packAvecComplement(int qteComplement) {
        if (complement != null && qteComplement > 0) {
            complement.retirerDuStock(qteComplement); // appel via attribut
            complement.appliquerRemise(0.02);         // appel via attribut
            complement.retirerRemise();               // appel via attribut
        }
    }
    // ------------ /Attributs de relation + appels via attributs ------------

    // Utils
    public void ajouterStock(int quantite) {
        if (quantite <= 0) return;
        stock += quantite;
    }

    public String getReference() { return reference; }
    public String getNom() { return nom; }
    public BigDecimal getTva() { return tva; }
    public int getStock() { return stock; }
    public BigDecimal getRemise() { return remise == null ? BigDecimal.ZERO : remise; }

    public void setPrixHT(BigDecimal prixHT) { this.prixHT = prixHT == null ? BigDecimal.ZERO : prixHT; }
    public void setTva(BigDecimal tva) { this.tva = tva == null ? BigDecimal.ZERO : tva; }

    @Override
    public String toString() {
        return "Produit{" +
                "ref='" + reference + '\'' +
                ", nom='" + nom + '\'' +
                ", prixHT=" + prixHT +
                ", tva=" + tva +
                ", stock=" + stock +
                ", remise=" + getRemise() +
                '}';
    }
}

package sn.isi.tontyn.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.model.Membre;
import sn.isi.tontyn.model.Role;
import sn.isi.tontyn.model.RoleGroupe;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.CotisationRepository;
import sn.isi.tontyn.repository.CycleRepository;
import sn.isi.tontyn.repository.MembreRepository;
import sn.isi.tontyn.repository.PaiementRepository;
import sn.isi.tontyn.repository.RecuRepository;
import sn.isi.tontyn.repository.UtilisateurRepository;

import java.util.Optional;

/**
 * Regles d'autorisation portant sur une tontine determinee.
 *
 * <p>Ce composant est expose au langage d'expression de Spring Security sous le
 * nom {@code secu}, ce qui permet de l'invoquer directement depuis les
 * annotations des controleurs :</p>
 *
 * <pre>{@code @PreAuthorize("@secu.gereTontine(#id)")}</pre>
 *
 * <p>Le principe retenu est celui du moindre privilege applique a la portee :
 * un utilisateur n'exerce de pouvoir de gestion que sur les tontines dont il
 * est gestionnaire, jamais sur celles auxquelles il n'appartient pas.
 * L'administrateur de la plateforme constitue la seule exception, justifiee
 * par ses obligations de supervision et de moderation.</p>
 */
@Component("secu")
public class SecuriteTontine {

    private final UtilisateurRepository utilisateurRepository;
    private final MembreRepository membreRepository;
    private final CycleRepository cycleRepository;
    private final CotisationRepository cotisationRepository;
    private final PaiementRepository paiementRepository;
    private final RecuRepository recuRepository;

    public SecuriteTontine(UtilisateurRepository utilisateurRepository,
                           MembreRepository membreRepository,
                           CycleRepository cycleRepository,
                           CotisationRepository cotisationRepository,
                           PaiementRepository paiementRepository,
                           RecuRepository recuRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.membreRepository = membreRepository;
        this.cycleRepository = cycleRepository;
        this.cotisationRepository = cotisationRepository;
        this.paiementRepository = paiementRepository;
        this.recuRepository = recuRepository;
    }

    // ------------------------------------------------------------------
    //  Utilisateur courant
    // ------------------------------------------------------------------

    /** Utilisateur authentifie pour la requete en cours, s'il existe. */
    @Transactional(readOnly = true)
    public Optional<Utilisateur> utilisateurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return Optional.empty();
        }
        return utilisateurRepository.findByTelephone(auth.getName());
    }

    /** Identifiant de l'utilisateur courant, ou {@code null} si aucun. */
    @Transactional(readOnly = true)
    public Long idCourant() {
        return utilisateurCourant().map(Utilisateur::getId).orElse(null);
    }

    /** Vrai si l'utilisateur courant est administrateur de la plateforme. */
    @Transactional(readOnly = true)
    public boolean estAdministrateurPlateforme() {
        return utilisateurCourant()
                .map(u -> u.getRole() == Role.ADMINISTRATEUR)
                .orElse(false);
    }

    // ------------------------------------------------------------------
    //  Regles de gestion, par point d'entree
    // ------------------------------------------------------------------

    /** Gestion d'une tontine : administrateur de la plateforme ou de cette tontine. */
    @Transactional(readOnly = true)
    public boolean gereTontine(Long tontineId) {
        if (tontineId == null) {
            return false;
        }
        if (estAdministrateurPlateforme()) {
            return true;
        }
        Long utilisateurId = idCourant();
        if (utilisateurId == null) {
            return false;
        }
        return membreRepository.findByTontineIdAndUtilisateurId(tontineId, utilisateurId)
                .map(m -> RoleGroupe.estGestionnaire(m.getRoleGroupe()))
                .orElse(false);
    }

    /** Appartenance simple : l'utilisateur participe a la tontine. */
    @Transactional(readOnly = true)
    public boolean appartientTontine(Long tontineId) {
        if (tontineId == null) {
            return false;
        }
        if (estAdministrateurPlateforme()) {
            return true;
        }
        Long utilisateurId = idCourant();
        return utilisateurId != null
                && membreRepository.existsByTontineIdAndUtilisateurId(tontineId, utilisateurId);
    }

    /**
     * Autorise la consultation d'un membre : administrateur de la plateforme,
     * ou toute personne appartenant a la meme tontine que ce membre (les
     * membres d'un meme groupe se voient entre eux, c'est la transparence de
     * base d'une tontine ; les tiers exterieurs au groupe, non).
     */
    @Transactional(readOnly = true)
    public boolean peutConsulterMembre(Long membreId) {
        if (membreId == null) {
            return false;
        }
        if (estAdministrateurPlateforme()) {
            return true;
        }
        return membreRepository.findById(membreId)
                .map(m -> appartientTontine(m.getTontine().getId()))
                .orElse(false);
    }

    /**
     * Autorise l'appel a {@code GET /api/membres} selon le filtre fourni :
     * par tontine (il faut y appartenir), par utilisateur (il faut etre
     * soi-meme), ou sans filtre (liste globale, dont le contenu est alors
     * restreint au niveau du service a ses propres tontines).
     */
    @Transactional(readOnly = true)
    public boolean peutListerMembres(Long tontineId, Long utilisateurId) {
        if (estAdministrateurPlateforme()) {
            return true;
        }
        if (tontineId != null) {
            return appartientTontine(tontineId);
        }
        if (utilisateurId != null) {
            return estLuiMeme(utilisateurId);
        }
        return true;
    }

    /** Gestion via un cycle : on remonte a la tontine qui le porte. */
    @Transactional(readOnly = true)
    public boolean gereCycle(Long cycleId) {
        if (cycleId == null) {
            return false;
        }
        return cycleRepository.findById(cycleId)
                .map(c -> gereTontine(c.getTontine().getId()))
                .orElse(false);
    }

    /** Gestion via un membre : on remonte a la tontine de rattachement. */
    @Transactional(readOnly = true)
    public boolean gereMembre(Long membreId) {
        if (membreId == null) {
            return false;
        }
        return membreRepository.findById(membreId)
                .map(m -> gereTontine(m.getTontine().getId()))
                .orElse(false);
    }

    /** Gestion via une cotisation : cotisation, puis cycle, puis tontine. */
    @Transactional(readOnly = true)
    public boolean gereCotisation(Long cotisationId) {
        if (cotisationId == null) {
            return false;
        }
        return cotisationRepository.findById(cotisationId)
                .map(c -> gereTontine(c.getCycle().getTontine().getId()))
                .orElse(false);
    }

    /** Gestion via un paiement : paiement, cotisation, cycle, puis tontine. */
    @Transactional(readOnly = true)
    public boolean gerePaiement(Long paiementId) {
        if (paiementId == null) {
            return false;
        }
        return paiementRepository.findById(paiementId)
                .map(p -> gereTontine(p.getCotisation().getCycle().getTontine().getId()))
                .orElse(false);
    }

    /** Gestion via un recu : recu, paiement, cotisation, cycle, puis tontine. */
    @Transactional(readOnly = true)
    public boolean gereRecu(Long recuId) {
        if (recuId == null) {
            return false;
        }
        return recuRepository.findById(recuId)
                .map(r -> gerePaiement(r.getPaiement().getId()))
                .orElse(false);
    }

    /** Acces aux donnees d'un utilisateur : lui-meme ou l'administrateur de la plateforme. */
    @Transactional(readOnly = true)
    public boolean estLuiMeme(Long utilisateurId) {
        if (utilisateurId == null) {
            return false;
        }
        if (estAdministrateurPlateforme()) {
            return true;
        }
        return utilisateurId.equals(idCourant());
    }

    /**
     * Le membre designe correspond-il a l'utilisateur courant ?
     * Utile pour autoriser une cotisation ou un paiement pour son propre compte.
     */
    @Transactional(readOnly = true)
    public boolean estSonPropreMembre(Long membreId) {
        if (membreId == null) {
            return false;
        }
        Long utilisateurId = idCourant();
        if (utilisateurId == null) {
            return false;
        }
        Optional<Membre> membre = membreRepository.findById(membreId);
        return membre.map(m -> utilisateurId.equals(m.getUtilisateur().getId())).orElse(false);
    }
}

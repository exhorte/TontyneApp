package sn.isi.tontyn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.PaiementRequest;
import sn.isi.tontyn.dto.PaiementResponse;
import sn.isi.tontyn.exception.ConflitMetierException;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.model.Cotisation;
import sn.isi.tontyn.model.MethodePaiement;
import sn.isi.tontyn.model.Paiement;
import sn.isi.tontyn.model.Recu;
import sn.isi.tontyn.repository.PaiementRepository;
import sn.isi.tontyn.repository.RecuRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final RecuRepository recuRepository;
    private final CotisationService cotisationService;
    private final RecuService recuService;
    private final NotificationService notificationService;

    public PaiementService(PaiementRepository paiementRepository,
                           RecuRepository recuRepository,
                           CotisationService cotisationService,
                           RecuService recuService,
                           NotificationService notificationService) {
        this.paiementRepository = paiementRepository;
        this.recuRepository = recuRepository;
        this.cotisationService = cotisationService;
        this.recuService = recuService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<PaiementResponse> lister() {
        return paiementRepository.findAll().stream().map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaiementResponse> listerParStatut(String statut) {
        return paiementRepository.findByStatut(statut).stream().map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public PaiementResponse obtenir(Long id) {
        return versReponse(chargerPaiement(id));
    }

    @Transactional(readOnly = true)
    public PaiementResponse obtenirParCotisation(Long cotisationId) {
        return versReponse(paiementRepository.findByCotisationId(cotisationId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Aucun paiement n'est rattache a la cotisation " + cotisationId + ".")));
    }

    /** Endpoint metier : initie le paiement d'une cotisation (statut INITIE). */
    public PaiementResponse initier(PaiementRequest req) {
        Cotisation cotisation = cotisationService.chargerCotisation(req.cotisationId());

        if ("PAYEE".equals(cotisation.getStatut())) {
            throw new ConflitMetierException("Cette cotisation est deja reglee.");
        }
        if (paiementRepository.existsByCotisationId(req.cotisationId())) {
            throw new ConflitMetierException(
                    "Un paiement est deja en cours pour cette cotisation.");
        }

        Paiement paiement = new Paiement();
        paiement.setCotisation(cotisation);
        // Somme exigible, penalite de retard comprise le cas echeant.
        paiement.setMontant(cotisation.montantDu());
        paiement.setMethode(req.methode());
        paiement.setReference(req.reference() != null && !req.reference().isBlank()
                ? req.reference() : genererReference(req.methode()));
        paiement.setStatut("INITIE");
        return versReponse(paiementRepository.save(paiement));
    }

    /**
     * Endpoint metier : confirme le paiement, solde la cotisation, emet le recu
     * et notifie le membre.
     */
    public PaiementResponse confirmer(Long id) {
        Paiement paiement = chargerPaiement(id);
        if ("CONFIRME".equals(paiement.getStatut())) {
            throw new ConflitMetierException("Ce paiement est deja confirme.");
        }
        if ("ANNULE".equals(paiement.getStatut())) {
            throw new ConflitMetierException("Ce paiement a ete annule : il ne peut plus etre "
                    + "confirme. Initiez un nouveau paiement.");
        }
        paiement.setStatut("CONFIRME");
        paiementRepository.save(paiement);

        Cotisation cotisation = paiement.getCotisation();
        cotisationService.marquerPayee(cotisation);

        Recu recu = recuService.genererSiAbsent(paiement);

        notificationService.envoyer(cotisation.getMembre().getUtilisateur(),
                "PAIEMENT_CONFIRME",
                "Votre cotisation de " + paiement.getMontant() + " FCFA pour la tontine \""
                        + cotisation.getCycle().getTontine().getNom() + "\" a bien ete enregistree. "
                        + "Numero de recu : " + recu.getNumero() + ".",
                "EMAIL");

        return PaiementResponse.from(paiement, recu.getId());
    }

    /** Annule un paiement non encore confirme. */
    public PaiementResponse annuler(Long id) {
        Paiement paiement = chargerPaiement(id);
        if ("CONFIRME".equals(paiement.getStatut())) {
            throw new ConflitMetierException(
                    "Un paiement confirme ne peut pas etre annule.");
        }
        paiement.setStatut("ANNULE");
        return versReponse(paiementRepository.save(paiement));
    }

    public void supprimer(Long id) {
        Paiement paiement = chargerPaiement(id);
        if (recuRepository.existsByPaiementId(id)) {
            throw new ConflitMetierException(
                    "Suppression impossible : un recu a ete emis pour ce paiement.");
        }
        paiementRepository.delete(paiement);
    }

    @Transactional(readOnly = true)
    public Paiement chargerPaiement(Long id) {
        return paiementRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Paiement", id));
    }

    private String genererReference(MethodePaiement methode) {
        String prefixe = methode == MethodePaiement.WAVE ? "WV" : "OM";
        return prefixe + "-" + UUID.randomUUID().toString()
                .substring(0, 10).toUpperCase();
    }

    private PaiementResponse versReponse(Paiement paiement) {
        Long recuId = recuRepository.findByPaiementId(paiement.getId())
                .map(Recu::getId).orElse(null);
        return PaiementResponse.from(paiement, recuId);
    }
}

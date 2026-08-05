package sn.isi.tontyn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.RecuResponse;
import sn.isi.tontyn.exception.ConflitMetierException;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.model.Paiement;
import sn.isi.tontyn.model.Recu;
import sn.isi.tontyn.repository.PaiementRepository;
import sn.isi.tontyn.repository.RecuRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RecuService {

    private final RecuRepository recuRepository;
    private final PaiementRepository paiementRepository;

    public RecuService(RecuRepository recuRepository, PaiementRepository paiementRepository) {
        this.recuRepository = recuRepository;
        this.paiementRepository = paiementRepository;
    }

    @Transactional(readOnly = true)
    public List<RecuResponse> lister() {
        return recuRepository.findAll().stream().map(RecuResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RecuResponse> listerParMembre(Long membreId) {
        return recuRepository.findByMembreId(membreId).stream().map(RecuResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RecuResponse obtenir(Long id) {
        return RecuResponse.from(recuRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Recu", id)));
    }

    @Transactional(readOnly = true)
    public RecuResponse obtenirParPaiement(Long paiementId) {
        return RecuResponse.from(recuRepository.findByPaiementId(paiementId)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Aucun recu n'a ete emis pour le paiement " + paiementId + ".")));
    }

    /** Endpoint metier : emet le recu d'un paiement confirme. */
    public RecuResponse genererPourPaiement(Long paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Paiement", paiementId));
        return RecuResponse.from(genererPourPaiement(paiement));
    }

    /** Variante interne : appelee lors de la confirmation d'un paiement. */
    public Recu genererPourPaiement(Paiement paiement) {
        if (!"CONFIRME".equals(paiement.getStatut())) {
            throw new ConflitMetierException("Le recu ne peut etre emis que pour un paiement "
                    + "confirme (statut actuel : " + paiement.getStatut() + ").");
        }
        Optional<Recu> existant = recuRepository.findByPaiementId(paiement.getId());
        if (existant.isPresent()) {
            throw new ConflitMetierException("Un recu a deja ete emis pour ce paiement (numero "
                    + existant.get().getNumero() + ").");
        }
        Recu recu = new Recu();
        recu.setPaiement(paiement);
        recu.setMontant(paiement.getMontant());
        recu.setNumero(genererNumero(paiement.getId()));
        return recuRepository.save(recu);
    }

    /** Emet le recu si absent, sans erreur s'il existe deja. */
    public Recu genererSiAbsent(Paiement paiement) {
        return recuRepository.findByPaiementId(paiement.getId())
                .orElseGet(() -> genererPourPaiement(paiement));
    }

    public void supprimer(Long id) {
        Recu recu = recuRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Recu", id));
        recuRepository.delete(recu);
    }

    /** Format : REC-AAAA-000001 */
    private String genererNumero(Long paiementId) {
        return String.format("REC-%d-%06d", LocalDate.now().getYear(), paiementId);
    }
}

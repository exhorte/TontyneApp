import { useRef, useState } from 'react'
import { assistantApi } from '../api/assistant.js'
import { useToast } from '../components/Toast.jsx'
import { normaliserErreur } from '../utils/errors.js'
import PageHeader from '../components/PageHeader.jsx'
import Button from '../components/Button.jsx'
import Field from '../components/Field.jsx'
import Alert from '../components/Alert.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Icone from '../components/Icone.jsx'

const SUGGESTIONS = ['Combien je dois ?', "C'est quand mon tour ?", "Ai-je des retards ?"]

/**
 * Panneau de discussion avec l'assistant (API DeepSeek cote serveur).
 *
 * Sans etat persiste : l'historique repart a zero a chaque rechargement de
 * page. Ce n'est pas un oubli, c'est le choix retenu pour la premiere
 * version (voir ETAT_DU_PROJET.md) — l'assistant ne connait que la situation
 * courante du membre, jamais l'echange precedent.
 */
export default function Assistant() {
  const toast = useToast()
  const [messages, setMessages] = useState([])
  const [question, setQuestion] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const zoneDefilement = useRef(null)

  const defilerVersLeBas = () => {
    requestAnimationFrame(() => {
      const zone = zoneDefilement.current
      if (zone) zone.scrollTo({ top: zone.scrollHeight, behavior: 'smooth' })
    })
  }

  const envoyer = async (evenement) => {
    evenement?.preventDefault()
    const texte = question.trim()
    if (!texte || envoi) return

    setMessages((m) => [...m, { role: 'utilisateur', texte }])
    setQuestion('')
    setEnvoi(true)
    defilerVersLeBas()

    try {
      const reponse = await assistantApi.demander(texte)
      setMessages((m) => [
        ...m,
        { role: 'assistant', texte: reponse.reponse, disponible: reponse.disponible },
      ])
    } catch (e) {
      const erreur = normaliserErreur(e)
      setMessages((m) => [...m, { role: 'assistant', texte: erreur.message, disponible: false }])
      toast.erreur(erreur.message)
    } finally {
      setEnvoi(false)
      defilerVersLeBas()
    }
  }

  const surTouche = (evenement) => {
    if (evenement.key === 'Enter' && !evenement.shiftKey) {
      evenement.preventDefault()
      envoyer()
    }
  }

  return (
    <>
      <PageHeader
        titre="Assistant"
        sousTitre="Posez une question simple sur votre tontine : ce que vous devez, votre tour, vos retards."
      />

      <Alert type="info">
        L'assistant ne connaît que votre propre situation, jamais celle des autres membres du
        groupe. Il ne peut rien modifier : ni confirmer un paiement, ni suspendre personne — pour
        ça, passez par les autres écrans.
      </Alert>

      <div className="carte assistant-panneau">
        <div className="assistant-fil" ref={zoneDefilement}>
          {messages.length === 0 ? (
            <EmptyState
              icone="assistant"
              titre="Aucune question pour l'instant"
              texte="Essayez par exemple :"
              action={
                <div className="assistant-suggestions">
                  {SUGGESTIONS.map((s) => (
                    <button
                      key={s}
                      type="button"
                      className="assistant-suggestion"
                      onClick={() => setQuestion(s)}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              }
            />
          ) : (
            messages.map((m, i) => (
              <div
                key={i}
                className={`assistant-bulle assistant-bulle--${m.role}${
                  m.disponible === false ? ' assistant-bulle--indisponible' : ''
                }`}
              >
                {m.texte}
              </div>
            ))
          )}
          {envoi && (
            <div className="assistant-bulle assistant-bulle--assistant assistant-bulle--attente">
              <span className="spinner spinner--petit" aria-hidden="true" />
              L'assistant réfléchit…
            </div>
          )}
        </div>

        <form className="assistant-saisie" onSubmit={envoyer}>
          <Field
            label="Votre question"
            nom="question"
            type="textarea"
            valeur={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={surTouche}
            disabled={envoi}
            maxLength={300}
            aide="300 caractères maximum. Entrée pour envoyer, Maj+Entrée pour une nouvelle ligne."
            requis
          />
          <Button
            type="submit"
            variante="principal"
            chargement={envoi}
            disabled={!question.trim()}
          >
            {
              <>
                <Icone nom="envoyer" taille={18} /> Envoyer
              </>
            }
          </Button>
        </form>
      </div>
    </>
  )
}

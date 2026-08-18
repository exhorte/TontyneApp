import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import Layout from './components/Layout.jsx'

import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Tontines from './pages/Tontines.jsx'
import TontineDetail from './pages/TontineDetail.jsx'
import Membres from './pages/Membres.jsx'
import Cycles from './pages/Cycles.jsx'
import CycleDetail from './pages/CycleDetail.jsx'
import Cotisations from './pages/Cotisations.jsx'
import Paiements from './pages/Paiements.jsx'
import Recus from './pages/Recus.jsx'
import Notifications from './pages/Notifications.jsx'
import Assistant from './pages/Assistant.jsx'
import Profil from './pages/Profil.jsx'
import AccesRefuse from './pages/AccesRefuse.jsx'
import NonTrouve from './pages/NonTrouve.jsx'

/**
 * Plan de routage.
 * Les routes publiques couvrent l'authentification ; tout le reste passe par
 * <ProtectedRoute> puis par le layout applicatif (barre + menu lateral).
 */
export default function App() {
  return (
    <Routes>
      {/* Publiques */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Protegees */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/tableau-de-bord" replace />} />
          <Route path="/tableau-de-bord" element={<Dashboard />} />

          <Route path="/tontines" element={<Tontines />} />
          <Route path="/tontines/:id" element={<TontineDetail />} />

          <Route path="/membres" element={<Membres />} />

          <Route path="/cycles" element={<Cycles />} />
          <Route path="/cycles/:id" element={<CycleDetail />} />

          <Route path="/cotisations" element={<Cotisations />} />
          <Route path="/paiements" element={<Paiements />} />
          <Route path="/recus" element={<Recus />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/assistant" element={<Assistant />} />
          <Route path="/profil" element={<Profil />} />

          <Route path="/acces-refuse" element={<AccesRefuse />} />
          <Route path="*" element={<NonTrouve />} />
        </Route>
      </Route>
    </Routes>
  )
}

export function OfficialHeader() {
  return (
    <div className="official-header">
      <div className="official-header-top">
        <div className="ministry-block">
          <img src="/armoiries-rdc.png" alt="Armoiries de la RDC" className="armoiries-logo" />
          <p className="ministry-text">
            MINISTÈRE
            <br />
            DE L'INTÉRIEUR, SÉCURITÉ,
            <br />
            DÉCENTRALISATION
            <br />
            ET AFFAIRES COUTUMIÈRES
          </p>
        </div>

        <div className="country-block">
          <p className="country-name">République Démocratique du Congo</p>
          <p className="office-name">OFFICE NATIONAL D'IDENTIFICATION DE LA POPULATION</p>
        </div>

        <img src="/onip-logo.png" alt="Logo ONIP" className="onip-logo" />
      </div>

      <div className="form-title-banner">
        <span>FICHE D'ADRESSAGE ET DE COMPOSITION DE MÉNAGE</span>
        <span className="form-code-badge">FACM01</span>
      </div>
    </div>
  )
}

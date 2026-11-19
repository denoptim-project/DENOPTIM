# DENOPTIM
# Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
# Licensed under the GNU Affero General Public License v3.

"""Build one 3D conformation from a SMILES string."""


def smiles_to_3d(parameters):
    """Embed ``parameters["smiles"]`` and relax it with a force field.

    Returns a dict with a V2000 molblock that includes explicit hydrogens and
    3D coordinates. Raises ``ValueError`` when the SMILES cannot be parsed or
    embedded.
    """
    try:
        from rdkit import Chem
        from rdkit.Chem import AllChem
    except ImportError as error:
        raise ValueError(
            "RDKit is not available to the socket server.") from error

    smiles = parameters.get("smiles") if isinstance(parameters, dict) else None
    if not isinstance(smiles, str) or smiles.strip() == "":
        raise ValueError("Parameter 'smiles' must be a non-empty string.")
    random_seed = parameters.get("randomSeed")
    if isinstance(random_seed, bool) or not isinstance(random_seed, int):
        raise ValueError("Parameter 'randomSeed' must be an integer.")

    molecule = Chem.MolFromSmiles(smiles)
    if molecule is None:
        raise ValueError("RDKit could not parse SMILES '%s'." % smiles)
    molecule = Chem.AddHs(molecule)

    embedder = AllChem.ETKDGv3()
    embedder.randomSeed = random_seed
    if AllChem.EmbedMolecule(molecule, embedder) != 0:
        embedder.useRandomCoords = True
        if AllChem.EmbedMolecule(molecule, embedder) != 0:
            raise ValueError("RDKit could not embed SMILES '%s'." % smiles)

    if AllChem.MMFFOptimizeMolecule(molecule) < 0:
        if AllChem.UFFOptimizeMolecule(molecule) < 0:
            raise ValueError(
                "RDKit could not relax the 3D model of SMILES '%s'." % smiles)

    return {"results": Chem.MolToMolBlock(molecule)}

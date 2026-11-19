# DENOPTIM
# Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
# Licensed under the GNU Affero General Public License v3.

"""Tasks the RDKit server can run.

Add a task by writing a function ``(parameters: dict) -> dict`` and registering
it in ``TASKS``. The Java client gains a matching method that sends the same
task name.
"""

from denoptim_rdkit.tasks.smiles_to_3d import smiles_to_3d

TASKS = {
    "smiles_to_3d": smiles_to_3d,
}

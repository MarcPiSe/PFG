import { Tooltip } from "../../../components/Tooltip"
import { UserSettings } from "../../../components/UserSettings"
import { Button } from "../../../components/Button"
import { RiLogoutBoxRLine } from "@remixicon/react"
import { RiAdminLine } from "react-icons/ri"
import { useAuth } from "../../../hooks/userAuth"

const Header = ({setAdminOpen}: {setAdminOpen: (open: boolean) => void}) => {
	const { logoutEndpoint, user } = useAuth();

	return (
		<div id="39" className="flex justify-end gap-2">
			<Tooltip side="left" content="Logout" triggerAsChild={true}>
				<Button variant="primary" className="p-2" onClick={logoutEndpoint}>
					<RiLogoutBoxRLine className="h-4 w-4" />
				</Button>
			</Tooltip>
			<UserSettings />
			{(user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN') && (
				<Tooltip side="left" content="Admin Panel" triggerAsChild={true}>
					<Button variant="primary" className="p-2" onClick={() => setAdminOpen(true)}> 
						<RiAdminLine className="h-4 w-4" />
					</Button>
				</Tooltip>
			)}
		</div>
	)
}

export default Header
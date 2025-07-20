import { Button } from "../Button";
import { RiFileTextLine, RiFolder3Line, RiArrowDownSLine } from "@remixicon/react";
import { useState } from "react";
import { useFileContextStore } from "../../store/fileContextStore";
import { Popover, PopoverTrigger, PopoverContent } from "../Popover";

const SORT_OPTIONS = [
	{ value: "nameUp", label: "A-Z" },
	{ value: "nameDown", label: "Z-A" },
	{ value: "updatedAtDown", label: "Last Modified" },
	{ value: "updatedAtUp", label: "First Modified" },
	{ value: "sizeDown", label: "Smallest" },
	{ value: "sizeUp", label: "Largest" },
	{ value: "createdAtDown", label: "First Created" },
	{ value: "createdAtUp", label: "Last Created" },
	{ value: "type", label: "Type" },
];

export const TabsSubHeader = () => {
	const [open, setOpen] = useState(false);

	const {filterByFiles, filterByFolders, setFilterByFiles, setFilterByFolders, sortBy, setSortBy} = useFileContextStore();

	const currentSort = SORT_OPTIONS.find(opt => opt.value === sortBy) || SORT_OPTIONS[0];

	return (
		<div id="32" className="flex justify-between w-full">
			<div className="flex gap-2">
				<Button onClick={() => setFilterByFiles(!filterByFiles)} variant={filterByFiles ? "primary" : 'secondary'}><RiFileTextLine />Archives</Button>
				<Button onClick={() => setFilterByFolders(!filterByFolders)} variant={filterByFolders ? "primary" : 'secondary'}><RiFolder3Line />Folders</Button>
			</div>
			<div className="flex gap-2 items-center">
				<Popover open={open} onOpenChange={setOpen}>
					<PopoverTrigger asChild>
						<Button variant="secondary" className="min-w-[120px] flex justify-between items-center gap-2">
							<span>Sort</span>
							<span className="text-indigo-300">{currentSort.label}</span>
							<RiArrowDownSLine className="ml-1" />
						</Button>
					</PopoverTrigger>
					<PopoverContent side="bottom" align="end" className="p-0 min-w-[180px]">
						<ul className="flex flex-col divide-y divide-gray-200">
							{SORT_OPTIONS.map(option => (
								<li key={option.value}>
									<button
										onClick={() => {
											setSortBy(option.value);
												setOpen(false);
										}}
										className={`w-full text-left text-indigo-300 px-4 py-2 hover:bg-gray-100 transition-colors flex items-center gap-2 ${sortBy === option.value ? 'text-indigo-600 font-semibold' : 'text-gray-800'}`}
									>
										{option.label}
										{sortBy === option.value && <span className="ml-auto w-2 h-2 bg-indigo-500 rounded-full" />}
									</button>
								</li>
							))}
						</ul>
					</PopoverContent>
				</Popover>
			</div>
		</div>
	)
};
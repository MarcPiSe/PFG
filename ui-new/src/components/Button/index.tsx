// Tremor Raw Button [v0.1.2]

import React from "react"
import { Slot } from "@radix-ui/react-slot"
import { RiLoader2Fill } from "@remixicon/react"
import { tv, type VariantProps } from "tailwind-variants"
import { cx, focusRing } from "../../lib/utils"

const buttonVariants = tv({
	base: [
		// base styles
		"relative inline-flex items-center justify-center whitespace-nowrap rounded-md border px-3 py-2 text-center text-sm font-medium shadow-sm transition-all duration-100 ease-in-out",
		// disabled state
		"disabled:pointer-events-none disabled:shadow-none",
		// focus state
		focusRing,
	],
	variants: {
		variant: {
			primary: [
				// border styles
				"border-transparent",
				// text color
				"text-white dark:text-gray-900",
				// background color
				"bg-gray-900 dark:bg-gray-50",
				// hover state
				"hover:bg-gray-800 dark:hover:bg-gray-200",
				// disabled state
				"disabled:bg-gray-100 disabled:text-gray-400",
				"disabled:dark:bg-gray-800 disabled:dark:text-gray-600",
			],
			secondary: [
				// border styles
				"border-gray-300 dark:border-gray-800",
				// text color
				"text-gray-900 dark:text-gray-50",
				// background color
				"bg-white dark:bg-gray-950",
				// hover state
				"hover:bg-gray-50 dark:hover:bg-gray-900/60",
				// disabled state
				"disabled:text-gray-400",
				"disabled:dark:text-gray-600",
			],
			light: [
				// base styles
				"shadow-none",
				// border styles
				"border-transparent",
				// text color
				"text-gray-900 dark:text-gray-50",
				// background color
				"bg-gray-200 dark:bg-gray-900",
				// hover state
				"hover:bg-gray-300/70 dark:hover:bg-gray-800/80",
				// disabled state
				"disabled:bg-gray-100 disabled:text-gray-400",
				"disabled:dark:bg-gray-800 disabled:dark:text-gray-600",
			],
			ghost: [
				// base styles
				"shadow-none",
				// border styles
				"border-transparent",
				// text color
				"text-gray-900 dark:text-gray-50",
				// hover state
				"bg-transparent hover:bg-gray-100 dark:hover:bg-gray-800/80",
				// disabled state
				"disabled:text-gray-400",
				"disabled:dark:text-gray-600",
			],
			destructive: [
				// text color
				"text-white",
				// border styles
				"border-transparent",
				// background color
				"bg-red-600 dark:bg-red-700",
				// hover state
				"hover:bg-red-700 dark:hover:bg-red-600",
				// disabled state
				"disabled:bg-red-300 disabled:text-white",
				"disabled:dark:bg-red-950 disabled:dark:text-red-400",
			],
		},
		size: {
			default: "h-9 px-4 py-2",
			sm: "h-8 rounded-md px-3 text-xs",
			lg: "h-10 rounded-md px-8",
			icon: "h-9 w-9",
		},
	},
	defaultVariants: {
		variant: "primary",
		size: "default",
	},
})

interface ButtonProps
	extends React.ComponentPropsWithoutRef<"button">,
	VariantProps<typeof buttonVariants> {
	asChild?: boolean
	isLoading?: boolean
	loadingText?: string
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
	(
		{
			asChild,
			isLoading = false,
			loadingText,
			className,
			disabled,
			variant,
			size,
			children,
			...props
		}: ButtonProps,
		forwardedRef,
	) => {

		const Component = asChild ? Slot : "button"
		
		const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
			
			props.onClick?.(e);
		};

		return (
			<Component
				ref={forwardedRef}
				className={cx(buttonVariants({ variant, size }), className)}
				disabled={disabled || isLoading}
				tremor-id="tremor-raw"
				onClick={handleClick}
				{...props}
			>
				{isLoading ? (
					<span className="pointer-events-none flex shrink-0 items-center justify-center gap-1.5">
						<RiLoader2Fill
							className="size-4 shrink-0 animate-spin"
							aria-hidden="true"
						/>
						<span className="sr-only">
							{loadingText ? loadingText : "Loading"}
						</span>
						{loadingText ? loadingText : children}
					</span>
				) : (
					children
				)}
			</Component>
		)
	},
)

Button.displayName = "Button"

export { Button, type ButtonProps }
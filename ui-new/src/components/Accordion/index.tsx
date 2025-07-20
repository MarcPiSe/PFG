// Tremor Raw Accordion [v0.0.1]

import React from "react"
import * as AccordionPrimitives from "@radix-ui/react-accordion"
import { RiArrowDropRightFill } from "@remixicon/react"
import { cx } from "../../lib/utils"


const Accordion = AccordionPrimitives.Root

Accordion.displayName = "AccordionItem"

const AccordionTrigger = React.forwardRef<
    React.ElementRef<typeof AccordionPrimitives.Trigger>,
    React.ComponentPropsWithoutRef<typeof AccordionPrimitives.Trigger> & {
        onArrowClick?: (e: React.MouseEvent) => void;
    }
>(({ className, children, onArrowClick, ...props }, forwardedRef) => {
    
    const handleArrowClick = (e: React.MouseEvent) => {
        
        e.stopPropagation();
        onArrowClick?.(e);
    };

    return (
        <AccordionPrimitives.Header className="flex">
            <AccordionPrimitives.Trigger
                className={cx(
                    "group flex flex-1 cursor-pointer items-center justify-between py-3 text-xs leading-none text-nowrap",
                    "text-gray-800 dark:text-gray-50",
                    "data-[disabled]:cursor-default data-[disabled]:text-gray-400 dark:data-[disabled]:text-gray-600",
                    "focus-visible:z-10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-500",
                    className,
                )}
                {...props}
                ref={forwardedRef}
            >
                <RiArrowDropRightFill
                    onClick={handleArrowClick}
                    className={cx(
                        "absolute -left-4",
                        "size-5 shrink-0 transition-transform duration-150 ease-[cubic-bezier(0.87,_0,_0.13,_1)] group-data-[state=open]:rotate-90",
                        "text-gray-800 dark:text-gray-600",
                        "group-data-[disabled]:text-gray-300 group-data-[disabled]:dark:text-gray-700",
                        "cursor-pointer"
                    )}
                    aria-hidden="true"
                    focusable="false"
                />
                {children}
            </AccordionPrimitives.Trigger>
        </AccordionPrimitives.Header>
    );
})

AccordionTrigger.displayName = "AccordionTrigger"

const AccordionContent = React.forwardRef<
	React.ElementRef<typeof AccordionPrimitives.Content>,
	React.ComponentPropsWithoutRef<typeof AccordionPrimitives.Content>
>(({ className, children, ...props }, forwardedRef) => {
    
    
    return (
        <AccordionPrimitives.Content
            ref={forwardedRef}
            className={cx(
                "transform-gpu data-[state=closed]:animate-accordionClose data-[state=open]:animate-accordionOpen pl-2",
            )}
            {...props}
        >
            <div id="6"
                className={cx(
                    // base
                    " text-sm",
                    // text color
                    "text-gray-800 dark:text-gray-200",
                    className,
                )}
            >
                {children}
            </div>
        </AccordionPrimitives.Content>
    );
})

AccordionContent.displayName = "AccordionContent"

const AccordionItem = React.forwardRef<
	React.ElementRef<typeof AccordionPrimitives.Item>,
	React.ComponentPropsWithoutRef<typeof AccordionPrimitives.Item>
>(({ className, ...props }, forwardedRef) => {
    
    
    return (
        <AccordionPrimitives.Item
            ref={forwardedRef}
            className={cx(
                className,
            )}
            tremor-id="tremor-raw"
            {...props}
        />
    );
})

AccordionItem.displayName = "AccordionItem"

export { Accordion, AccordionContent, AccordionItem, AccordionTrigger }